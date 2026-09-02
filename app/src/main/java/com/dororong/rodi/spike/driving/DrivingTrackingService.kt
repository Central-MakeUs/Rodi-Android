package com.dororong.rodi.spike.driving

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.driving.DrivingLocationSample
import com.dororong.rodi.core.domain.model.driving.DrivingSession
import com.dororong.rodi.core.domain.model.driving.DrivingSessionStatus
import com.dororong.rodi.core.domain.usecase.driving.DrivingProgressAccumulator
import com.dororong.rodi.core.domain.usecase.driving.EndDrivingSessionUseCase
import com.dororong.rodi.core.domain.usecase.driving.MarkDrivingArrivedUseCase
import com.dororong.rodi.core.domain.usecase.driving.RadiusArrivalPolicy
import com.dororong.rodi.core.domain.usecase.driving.RouteProgressTracker
import com.dororong.rodi.core.domain.usecase.driving.StartDrivingSessionUseCase
import com.dororong.rodi.core.domain.usecase.driving.UpdateDrivingProgressUseCase
import com.dororong.rodi.core.domain.usecase.driving.distanceTo
import com.dororong.rodi.core.domain.usecase.practice.ConfirmPracticeArrivalUseCase
import com.dororong.rodi.feature.home.location.rawCurrentLocationUpdates
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

private const val NOTIFICATION_UPDATE_DISTANCE_METERS = 20.0
private const val NOTIFICATION_UPDATE_INTERVAL_MILLIS = 15_000L

// 목적지 반경 도달을 완료 조건으로 같이 쓰되, 출발-도착이 가까운 순환/왕복 코스에서
// 출발하자마자 "도착"으로 잡히는 걸 막기 위한 안전장치. 코스 길이 자체가 짧으면 반경
// fallback을 아예 안 쓰고, 인정거리를 어느 정도 쌓은 뒤에만 허용한다.
private const val MIN_START_TO_DESTINATION_METERS_FOR_ARRIVAL_FALLBACK = 300.0
private const val MIN_RECOGNIZED_DISTANCE_METERS_FOR_ARRIVAL_FALLBACK = 50.0

@AndroidEntryPoint
internal class DrivingTrackingService : Service() {
    private sealed interface Command {
        data class Start(val session: DrivingSession) : Command
        data class Stop(val sessionId: String?) : Command
        data class Arrive(val session: DrivingSession, val traveledDistanceMeters: Double) : Command
    }

    private val commandChannel = Channel<Command>(Channel.UNLIMITED)

    @Inject lateinit var startDrivingSession: StartDrivingSessionUseCase
    @Inject lateinit var updateDrivingProgress: UpdateDrivingProgressUseCase
    @Inject lateinit var markDrivingArrived: MarkDrivingArrivedUseCase
    @Inject lateinit var endDrivingSession: EndDrivingSessionUseCase
    @Inject lateinit var confirmPracticeArrivalUseCase: ConfirmPracticeArrivalUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val notificationManager by lazy { NotificationManagerCompat.from(this) }
    private var trackingJob: Job? = null
    private var activeSession: DrivingSession? = null
    @Volatile private var isFinishing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            for (command in commandChannel) {
                when (command) {
                    is Command.Start -> handleStartCommand(command.session)
                    is Command.Stop -> handleStopCommand(command.sessionId)
                    is Command.Arrive -> handleArrival(command.session, command.traveledDistanceMeters)
                }
            }
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_START -> handleStartAction(intent)
            ACTION_STOP -> commandChannel.trySend(Command.Stop(intent.getStringExtra(EXTRA_SESSION_ID)))
        }
        return START_NOT_STICKY
    }

    /**
     * startForegroundService()로 시작된 서비스는 시스템 제한 시간(약 5초) 안에
     * startForeground()를 호출해야 한다. 세션 파싱·권한 체크와 startForeground 호출을
     * 커맨드 채널의 비동기 소비자로 미루면 그 지연만으로 ForegroundServiceDidNotStartInTimeException이
     * 날 수 있어, 이 검증·승격은 onStartCommand에서 동기적으로 끝낸다.
     */
    private fun handleStartAction(intent: Intent) {
        if (activeSession != null || isFinishing) return
        val session = intent.toDrivingSession() ?: run {
            stopSelf()
            return
        }
        if (!hasLocationPermission()) {
            stopSelf()
            return
        }
        activeSession = session
        DrivingNotificationFactory.createChannels(this)
        val notification = DrivingNotificationFactory.ongoing(this, session, 0.0)
        try {
            ServiceCompat.startForeground(
                this,
                DrivingNotificationFactory.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } catch (error: RuntimeException) {
            Timber.e(error, "Driving foreground service could not start.")
            activeSession = null
            stopSelf()
            return
        }
        commandChannel.trySend(Command.Start(session))
    }

    override fun onDestroy() {
        trackingJob?.cancel()
        commandChannel.close()
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun handleStartCommand(session: DrivingSession) {
        try {
            startDrivingSession(session)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.e(error, "Driving tracking stopped unexpectedly.")
            finishSession(session.id, removeNotification = true, clearSession = true)
            return
        }
        trackingJob = serviceScope.launch {
            try {
                collectLocations(session)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Timber.e(error, "Driving tracking stopped unexpectedly.")
                trackingJob = null
                finishSession(session.id, removeNotification = true, clearSession = true)
            }
        }
    }

    private suspend fun collectLocations(session: DrivingSession) {
        val requiredDistanceMeters = session.requiredDistanceMeters
        if (session.courseRoute.size >= 2 && requiredDistanceMeters != null && requiredDistanceMeters > 0) {
            collectLocationsWithRouteProgress(session, session.courseRoute)
        } else {
            collectLocationsWithLegacyModel(session)
        }
    }

    private suspend fun collectLocationsWithRouteProgress(
        session: DrivingSession,
        route: List<GeoPoint>,
    ) {
        val tracker = RouteProgressTracker(
            route = route,
            requiredDistanceMeters = session.requiredDistanceMeters ?: 0,
        )
        val destinationArrivalPolicy = RadiusArrivalPolicy()
        val startToDestinationMeters = route.first().distanceTo(session.destination)
        val destinationFallbackEnabled =
            startToDestinationMeters >= MIN_START_TO_DESTINATION_METERS_FOR_ARRIVAL_FALLBACK
        var consecutiveDestinationMatches = 0
        var lastPublishedDistance = 0.0
        var lastPublishedAt = SystemClock.elapsedRealtime()
        rawCurrentLocationUpdates().collect { location ->
            if (isFinishing || activeSession?.id != session.id) return@collect
            if (!canKeepTrackingVisible()) {
                trackingJob = null
                finishSession(session.id, removeNotification = true, clearSession = true)
                return@collect
            }
            val sample = location.toDrivingLocationSample()
            val progress = tracker.add(sample)
            var hasArrived = progress.isComplete
            if (
                !hasArrived &&
                destinationFallbackEnabled &&
                progress.recognizedDistanceMeters >= MIN_RECOGNIZED_DISTANCE_METERS_FOR_ARRIVAL_FALLBACK
            ) {
                val destinationArrival = destinationArrivalPolicy.evaluate(
                    sample = sample,
                    destination = session.destination,
                    previousConsecutiveMatches = consecutiveDestinationMatches,
                )
                consecutiveDestinationMatches = destinationArrival.consecutiveMatches
                hasArrived = destinationArrival.hasArrived
            }
            if (hasArrived) {
                commandChannel.trySend(Command.Arrive(session, progress.recognizedDistanceMeters))
                return@collect
            }
            val now = SystemClock.elapsedRealtime()
            if (
                progress.recognizedDistanceMeters - lastPublishedDistance >= NOTIFICATION_UPDATE_DISTANCE_METERS ||
                now - lastPublishedAt >= NOTIFICATION_UPDATE_INTERVAL_MILLIS
            ) {
                updateDrivingProgress(session.id, progress.recognizedDistanceMeters)
                publishOngoing(session, progress.recognizedDistanceMeters)
                lastPublishedDistance = progress.recognizedDistanceMeters
                lastPublishedAt = now
            }
        }
    }

    private suspend fun collectLocationsWithLegacyModel(session: DrivingSession) {
        val arrivalPolicy = RadiusArrivalPolicy()
        val progressAccumulator = DrivingProgressAccumulator()
        var consecutiveMatches = 0
        var lastPublishedDistance = 0.0
        var lastPublishedAt = SystemClock.elapsedRealtime()
        rawCurrentLocationUpdates().collect { location ->
            if (isFinishing || activeSession?.id != session.id) return@collect
            if (!canKeepTrackingVisible()) {
                trackingJob = null
                finishSession(session.id, removeNotification = true, clearSession = true)
                return@collect
            }
            val sample = location.toDrivingLocationSample()
            val traveledDistance = progressAccumulator.add(sample)
            val arrival = arrivalPolicy.evaluate(
                sample = sample,
                destination = session.destination,
                previousConsecutiveMatches = consecutiveMatches,
            )
            consecutiveMatches = arrival.consecutiveMatches
            if (arrival.hasArrived) {
                commandChannel.trySend(Command.Arrive(session, traveledDistance))
                return@collect
            }
            val now = SystemClock.elapsedRealtime()
            if (
                traveledDistance - lastPublishedDistance >= NOTIFICATION_UPDATE_DISTANCE_METERS ||
                now - lastPublishedAt >= NOTIFICATION_UPDATE_INTERVAL_MILLIS
            ) {
                updateDrivingProgress(session.id, traveledDistance)
                publishOngoing(session, traveledDistance)
                lastPublishedDistance = traveledDistance
                lastPublishedAt = now
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun handleArrival(
        session: DrivingSession,
        traveledDistanceMeters: Double,
    ) {
        if (isFinishing) return
        isFinishing = true
        val arrivedAt = System.currentTimeMillis()
        val transitioned = runCatching {
            markDrivingArrived(session.id, arrivedAt, traveledDistanceMeters)
        }.getOrElse { error ->
            isFinishing = false
            Timber.e(error, "Driving arrival could not be persisted.")
            return
        }
        if (!transitioned) {
            isFinishing = false
            return
        }
        confirmPracticeArrival(session.placeId)
        val arrivedSession = session.copy(
            arrivedAtEpochMillis = arrivedAt,
            traveledDistanceMeters = traveledDistanceMeters,
            status = DrivingSessionStatus.ARRIVED,
            isArrivalNoticePending = true,
        )
        activeSession = null
        stopForeground(STOP_FOREGROUND_DETACH)
        notificationManager.notify(
            DrivingNotificationFactory.NOTIFICATION_ID,
            DrivingNotificationFactory.arrival(this, arrivedSession),
        )
        trackingJob?.cancel()
        stopSelf()
    }

    private suspend fun confirmPracticeArrival(placeId: Long) {
        try {
            confirmPracticeArrivalUseCase(placeId)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.e(error, "Could not confirm practice arrival on the active session.")
        }
    }

    private suspend fun handleStopCommand(sessionId: String?) {
        val active = activeSession ?: run {
            stopSelf()
            return
        }
        if (sessionId != null && sessionId != active.id) return
        if (isFinishing) return
        isFinishing = true
        finishSession(active.id, removeNotification = true, clearSession = true)
    }

    private suspend fun finishSession(
        sessionId: String,
        removeNotification: Boolean,
        clearSession: Boolean,
    ) {
        isFinishing = true
        trackingJob?.cancel()
        if (clearSession) runCatching { endDrivingSession(sessionId) }
            .onFailure { Timber.e(it, "Driving session could not be cleared.") }
        activeSession = null
        if (removeNotification) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            notificationManager.cancel(DrivingNotificationFactory.NOTIFICATION_ID)
        }
        stopSelf()
    }

    @SuppressLint("MissingPermission")
    private fun publishOngoing(
        session: DrivingSession,
        traveledDistanceMeters: Double,
    ) {
        notificationManager.notify(
            DrivingNotificationFactory.NOTIFICATION_ID,
            DrivingNotificationFactory.ongoing(this, session, traveledDistanceMeters),
        )
    }

    private fun hasLocationPermission(): Boolean = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ).any { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun canKeepTrackingVisible(): Boolean {
        if (!notificationManager.areNotificationsEnabled()) return false
        val manager = getSystemService(NotificationManager::class.java)
        return manager.getNotificationChannel(DrivingNotificationFactory.ONGOING_CHANNEL_ID)
            ?.importance != NotificationManager.IMPORTANCE_NONE
    }

    companion object {
        const val ACTION_START = "com.dororong.rodi.action.START_DRIVING"
        const val ACTION_STOP = "com.dororong.rodi.action.STOP_DRIVING"
        const val ACTION_OPEN_ARRIVAL = "com.dororong.rodi.action.OPEN_DRIVING_ARRIVAL"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_PLACE_ID = "place_id"
        const val EXTRA_PLACE_NAME = "place_name"
        const val EXTRA_DESTINATION_LAT = "destination_lat"
        const val EXTRA_DESTINATION_LNG = "destination_lng"
        const val EXTRA_PLANNED_DISTANCE_METERS = "planned_distance_meters"
        const val EXTRA_COURSE_ROUTE_LAT_LNG = "course_route_lat_lng"
        const val EXTRA_REQUIRED_DISTANCE_METERS = "required_distance_meters"
    }
}

private fun Intent.toDrivingSession(): DrivingSession? {
    val sessionId = getStringExtra(DrivingTrackingService.EXTRA_SESSION_ID) ?: return null
    val placeName = getStringExtra(DrivingTrackingService.EXTRA_PLACE_NAME) ?: return null
    val placeId = getLongExtra(DrivingTrackingService.EXTRA_PLACE_ID, Long.MIN_VALUE)
    val destinationLat = numberExtra(DrivingTrackingService.EXTRA_DESTINATION_LAT) ?: return null
    val destinationLng = numberExtra(DrivingTrackingService.EXTRA_DESTINATION_LNG) ?: return null
    if (placeId == Long.MIN_VALUE) return null
    val plannedDistance = getIntExtra(
        DrivingTrackingService.EXTRA_PLANNED_DISTANCE_METERS,
        -1,
    ).takeIf { it > 0 }
    val requiredDistance = getIntExtra(
        DrivingTrackingService.EXTRA_REQUIRED_DISTANCE_METERS,
        -1,
    ).takeIf { it > 0 }
    val route = getDoubleArrayExtra(DrivingTrackingService.EXTRA_COURSE_ROUTE_LAT_LNG)
        ?.toList()
        ?.chunked(2)
        ?.mapNotNull { pair -> pair.getOrNull(0)?.let { lat -> pair.getOrNull(1)?.let { lng -> GeoPoint(lat, lng) } } }
        .orEmpty()
    return DrivingSession(
        id = sessionId,
        placeId = placeId,
        placeName = placeName,
        destination = GeoPoint(destinationLat, destinationLng),
        plannedDistanceMeters = plannedDistance,
        startedAtEpochMillis = System.currentTimeMillis(),
        arrivedAtEpochMillis = null,
        traveledDistanceMeters = 0.0,
        status = DrivingSessionStatus.ACTIVE,
        isArrivalNoticePending = false,
        courseRoute = route,
        requiredDistanceMeters = requiredDistance,
    )
}

private fun Intent.numberExtra(key: String): Double? =
    getDoubleExtra(key, Double.NaN).takeUnless(Double::isNaN)
        ?: getStringExtra(key)?.toDoubleOrNull()

private fun Location.toDrivingLocationSample(): DrivingLocationSample = DrivingLocationSample(
    point = GeoPoint(latitude, longitude),
    accuracyMeters = accuracy,
    elapsedRealtimeMillis = elapsedRealtimeNanos / 1_000_000L,
)
