package com.dororong.rodi.spike.driving

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dororong.rodi.core.domain.model.course.RouteResult
import com.dororong.rodi.core.domain.model.driving.DrivingSession
import com.dororong.rodi.core.domain.model.driving.DrivingSessionStatus
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.place.PlaceWaypointType
import com.dororong.rodi.core.domain.usecase.driving.requiredCertifiedDistanceMeters
import java.util.UUID

object DrivingTrackingController {
    fun start(
        context: Context,
        place: PlaceDetail,
        route: RouteResult? = null,
    ): Result<String> = runCatching {
        requireTrackingPermissions(context)
        DrivingNotificationFactory.createChannels(context)
        requireNotificationsEnabled(context)

        val destination = place.course?.waypoints.orEmpty()
            .firstOrNull { it.type == PlaceWaypointType.DESTINATION }
            ?.point
            ?: place.point
        val sessionId = UUID.randomUUID().toString()
        val routePoints = route?.points?.takeIf { it.size >= 2 }
            ?: place.course?.waypoints.orEmpty()
                .sortedBy { it.sequence }
                .map { it.point }
                .takeIf { it.size >= 2 }
        val courseDistanceMeters = route?.totalDistanceMeters?.takeIf { it > 0 }
            ?: place.course?.distanceMeters?.takeIf { it > 0 }
        val requiredDistanceMeters = if (place.type == PlaceType.COURSE) {
            courseDistanceMeters?.let(::requiredCertifiedDistanceMeters)
        } else {
            null
        }
        ContextCompat.startForegroundService(
            context,
            startIntent(
                context = context,
                session = DrivingSession(
                    id = sessionId,
                    placeId = place.id,
                    placeName = place.name,
                    destination = destination,
                    plannedDistanceMeters = courseDistanceMeters,
                    startedAtEpochMillis = System.currentTimeMillis(),
                    arrivedAtEpochMillis = null,
                    traveledDistanceMeters = 0.0,
                    status = DrivingSessionStatus.ACTIVE,
                    isArrivalNoticePending = false,
                    requiredDistanceMeters = requiredDistanceMeters,
                    courseRoute = routePoints.orEmpty(),
                ),
            ),
        )
        sessionId
    }

    /**
     * 알림이 차단돼 서비스를 멈춘 뒤, 사용자가 앱에서 명시적으로 재개할 때만 호출한다.
     */
    fun resume(
        context: Context,
        session: DrivingSession,
    ): Result<Unit> = runCatching {
        check(session.status == DrivingSessionStatus.ACTIVE) {
            "완료된 운전 세션은 다시 시작할 수 없어요."
        }
        requireTrackingPermissions(context)
        DrivingNotificationFactory.createChannels(context)
        requireNotificationsEnabled(context)
        ContextCompat.startForegroundService(context, startIntent(context, session))
    }

    fun stop(
        context: Context,
        sessionId: String? = null,
    ) {
        val intent = Intent(context, DrivingTrackingService::class.java)
            .setAction(DrivingTrackingService.ACTION_STOP)
        if (sessionId != null) intent.putExtra(DrivingTrackingService.EXTRA_SESSION_ID, sessionId)
        context.startService(intent)
    }

    fun clearArrival(context: Context) {
        NotificationManagerCompat.from(context).cancel(DrivingNotificationFactory.NOTIFICATION_ID)
    }

    private fun requireTrackingPermissions(context: Context) {
        val hasLocation = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ).any { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
        check(hasLocation) { "위치 권한을 허용해야 운전 상태를 추적할 수 있어요." }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            check(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED,
            ) {
                "알림 권한을 허용해야 운전 상태를 안전하게 표시할 수 있어요."
            }
        }
    }

    private fun requireNotificationsEnabled(context: Context) {
        check(NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            "알림이 꺼져 있어 운전 상태를 추적할 수 없어요. 설정에서 알림을 켜 주세요."
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = manager.getNotificationChannel(DrivingNotificationFactory.ONGOING_CHANNEL_ID)
        check(
            channel == null || channel.importance > NotificationManager.IMPORTANCE_MIN,
        ) {
            "운전 상태 알림 채널이 꺼져 있어요. 설정에서 알림을 켜 주세요."
        }
    }

    private fun startIntent(
        context: Context,
        session: DrivingSession,
    ): Intent = Intent(context, DrivingTrackingService::class.java)
        .setAction(DrivingTrackingService.ACTION_START)
        .putExtra(DrivingTrackingService.EXTRA_SESSION_ID, session.id)
        .putExtra(DrivingTrackingService.EXTRA_PLACE_ID, session.placeId)
        .putExtra(DrivingTrackingService.EXTRA_PLACE_NAME, session.placeName)
        .putExtra(DrivingTrackingService.EXTRA_DESTINATION_LAT, session.destination.lat)
        .putExtra(DrivingTrackingService.EXTRA_DESTINATION_LNG, session.destination.lng)
        .putExtra(
            DrivingTrackingService.EXTRA_PLANNED_DISTANCE_METERS,
            session.plannedDistanceMeters ?: -1,
        )
        .putExtra(
            DrivingTrackingService.EXTRA_REQUIRED_DISTANCE_METERS,
            session.requiredDistanceMeters ?: -1,
        )
        .putExtra(
            DrivingTrackingService.EXTRA_ROUTE_LATITUDES,
            session.courseRoute.map { it.lat }.toDoubleArray(),
        )
        .putExtra(
            DrivingTrackingService.EXTRA_ROUTE_LONGITUDES,
            session.courseRoute.map { it.lng }.toDoubleArray(),
        )
        .putExtra(
            DrivingTrackingService.EXTRA_STARTED_AT_EPOCH_MILLIS,
            session.startedAtEpochMillis,
        )
        .putExtra(
            DrivingTrackingService.EXTRA_TRAVELED_DISTANCE_METERS,
            session.traveledDistanceMeters,
        )
        .putExtra(
            DrivingTrackingService.EXTRA_IN_COURSE_SCOPE,
            session.isInCourseScope,
        )
}
