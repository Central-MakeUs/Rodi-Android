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
import com.dororong.rodi.core.domain.usecase.driving.StartDrivingSessionUseCase
import com.dororong.rodi.core.domain.usecase.driving.UpdateDrivingProgressUseCase
import com.dororong.rodi.core.domain.usecase.practice.GetActivePracticeSessionUseCase
import com.dororong.rodi.core.domain.usecase.practice.SaveActivePracticeSessionUseCase
import com.dororong.rodi.feature.home.location.rawCurrentLocationUpdates
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

private const val NOTIFICATION_UPDATE_DISTANCE_METERS = 20.0
private const val NOTIFICATION_UPDATE_INTERVAL_MILLIS = 15_000L

@AndroidEntryPoint
internal class DrivingTrackingService : Service() {
    @Inject lateinit var startDrivingSession: StartDrivingSessionUseCase
    @Inject lateinit var updateDrivingProgress: UpdateDrivingProgressUseCase
    @Inject lateinit var markDrivingArrived: MarkDrivingArrivedUseCase
    @Inject lateinit var endDrivingSession: EndDrivingSessionUseCase
    @Inject lateinit var getActivePracticeSession: GetActivePracticeSessionUseCase
    @Inject lateinit var saveActivePracticeSession: SaveActivePracticeSessionUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val notificationManager by lazy { NotificationManagerCompat.from(this) }
    private var trackingJob: Job? = null
    private var activeSession: DrivingSession? = null
    private var isFinishing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_START -> startTracking(intent)
            ACTION_STOP -> stopTracking(intent.getStringExtra(EXTRA_SESSION_ID))
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        trackingJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startTracking(intent: Intent) {
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
        trackingJob = serviceScope.launch {
            try {
                startDrivingSession(session)
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
                handleArrival(session, traveledDistance)
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
        confirmPracticeArrival()
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

    /**
     * ActivePracticeSession(홈의 방문 확인 다이얼로그가 보는 상태)은 이 DrivingSession과 별개
     * DataStore다. GPS로 실제 도착을 확인한 순간 여기서 바로 표시해두지 않으면, 홈은 여전히
     * "10분 경과" 휴리스틱만 보고 이미 도착한 사용자에게도 "계속 측정 중이신가요?"를 묻는다.
     */
    private suspend fun confirmPracticeArrival() {
        runCatching {
            val practiceSession = getActivePracticeSession() ?: return@runCatching
            saveActivePracticeSession(practiceSession.copy(isArrivalConfirmed = true))
        }.onFailure { error ->
            Timber.e(error, "Could not confirm practice arrival on the active session.")
        }
    }

    private fun stopTracking(sessionId: String?) {
        val active = activeSession ?: run {
            stopSelf()
            return
        }
        if (sessionId != null && sessionId != active.id) return
        if (isFinishing) return
        isFinishing = true
        serviceScope.launch {
            finishSession(active.id, removeNotification = true, clearSession = true)
        }
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
