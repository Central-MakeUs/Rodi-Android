package com.dororong.rodi.spike.driving

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.dororong.rodi.MainActivity
import com.dororong.rodi.R
import com.dororong.rodi.core.domain.model.driving.DrivingSession
import com.dororong.rodi.core.ui.theme.LightRodiColors
import java.util.Locale

internal enum class DrivingNotificationStyle {
    STANDARD,
    PROGRESS_STYLE,
}

internal object DrivingNotificationStylePolicy {
    fun forApi(apiLevel: Int): DrivingNotificationStyle =
        if (apiLevel >= Build.VERSION_CODES.BAKLAVA) {
            DrivingNotificationStyle.PROGRESS_STYLE
        } else {
            DrivingNotificationStyle.STANDARD
        }

    fun requestsPromotion(apiLevel: Int): Boolean = apiLevel >= Build.VERSION_CODES.BAKLAVA
}

internal object DrivingNotificationFactory {
    const val NOTIFICATION_ID = 4_210
    const val ONGOING_CHANNEL_ID = "driving_tracking"
    private const val ARRIVAL_CHANNEL_ID = "driving_arrival"
    private const val PROGRESS_MAX = 100

    private val primaryColor = LightRodiColors.primary600.toArgb()
    private val progressColor = LightRodiColors.primary600.toArgb()
    private val progressTrackColor = LightRodiColors.gray300.toArgb()

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                ONGOING_CHANNEL_ID,
                "운전 상태",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "운전 연습 중 위치 추적 상태"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                ARRIVAL_CHANNEL_ID,
                "연습 완료",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "운전 연습 완료 안내"
                setShowBadge(false)
            },
        )
    }

    fun ongoing(
        context: Context,
        session: DrivingSession,
        traveledDistanceMeters: Double,
    ): Notification {
        val progress = session.plannedDistanceMeters
            ?.takeIf { it > 0 }
            ?.let { planned ->
                ((traveledDistanceMeters / planned) * PROGRESS_MAX)
                    .toInt()
                    .coerceIn(0, 99)
            }
        val isOnTheWayToCourse = traveledDistanceMeters <= 0.0
        val title = if (isOnTheWayToCourse) {
            "연습 코스로 이동하고 있어요"
        } else {
            "${session.placeName.trimEnd().removeSuffix("코스").trimEnd()} 코스 주행 중"
        }
        val message = if (isOnTheWayToCourse) {
            "코스에 도착하면 Rodi가 주행을 기록해 드릴게요."
        } else {
            "Rodi가 코스 주행을 확인하고 있어요."
        }
        val style = DrivingNotificationStylePolicy.forApi(Build.VERSION.SDK_INT)
        val builder = NotificationCompat.Builder(context, ONGOING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setLargeIcon(brandIcon(context))
            .setColor(progressColor)
            .setContentIntent(openAppIntent(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setWhen(session.startedAtEpochMillis)
            .setShowWhen(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "운전 종료",
                stopServiceIntent(context, session.id),
            )

        if (DrivingNotificationStylePolicy.requestsPromotion(Build.VERSION.SDK_INT)) {
            builder.setRequestPromotedOngoing(true)
        }

        if (style == DrivingNotificationStyle.PROGRESS_STYLE && !isOnTheWayToCourse) {
            val progressStyle = NotificationCompat.ProgressStyle()
                .setProgress(progress ?: 0)
                .setStyledByProgress(false)
                .setProgressSegments(progressSegments(progress ?: 0))
                .setProgressTrackerIcon(progressTrackerIcon(context))
            builder
                .setShortCriticalText(
                    "${progress ?: 0}% · ${traveledDistanceMeters.toDistanceText()}",
                )
                .setStyle(progressStyle)
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(message))
            if (!isOnTheWayToCourse) {
                builder.setProgress(PROGRESS_MAX, progress ?: 0, false)
            }
        }
        return builder.build()
    }

    fun arrival(
        context: Context,
        session: DrivingSession,
    ): Notification {
        val message = "오늘도 한 걸음 성장했어요.\nRodi로 돌아가 기록을 남겨주세요."
        return NotificationCompat.Builder(context, ARRIVAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("오늘의 운전연습을 완료했어요!")
            .setContentText(message)
            .setSubText("RODI")
            .setLargeIcon(brandIcon(context))
            .setColor(primaryColor)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .setOngoing(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setOnlyAlertOnce(false)
            .setWhen(session.arrivedAtEpochMillis ?: System.currentTimeMillis())
            .setShowWhen(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "기록하러 가기",
                openAppIntent(context),
            )
            .build()
    }

    private fun progressSegments(progress: Int): List<NotificationCompat.ProgressStyle.Segment> {
        val completed = progress.coerceIn(0, PROGRESS_MAX)
        if (completed == 0) {
            return listOf(
                NotificationCompat.ProgressStyle.Segment(PROGRESS_MAX)
                    .setColor(progressTrackColor),
            )
        }
        val remaining = PROGRESS_MAX - completed
        return buildList {
            add(
                NotificationCompat.ProgressStyle.Segment(completed)
                    .setColor(progressColor),
            )
            if (remaining > 0) {
                add(
                    NotificationCompat.ProgressStyle.Segment(remaining)
                        .setColor(progressTrackColor),
                )
            }
        }
    }

    private fun brandIcon(context: Context) =
        BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)

    private fun progressTrackerIcon(context: Context): IconCompat =
        IconCompat.createWithResource(context, R.drawable.ic_driving_progress_tracker)

    private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java)
            .setAction(DrivingTrackingService.ACTION_OPEN_ARRIVAL)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun stopServiceIntent(
        context: Context,
        sessionId: String,
    ): PendingIntent = PendingIntent.getService(
        context,
        sessionId.hashCode(),
        Intent(context, DrivingTrackingService::class.java)
            .setAction(DrivingTrackingService.ACTION_STOP)
            .putExtra(DrivingTrackingService.EXTRA_SESSION_ID, sessionId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

private fun Double.toDistanceText(): String =
    if (this >= 1_000) {
        String.format(Locale.KOREA, "%.1f km", this / 1_000)
    } else {
        "${toInt()} m"
    }
