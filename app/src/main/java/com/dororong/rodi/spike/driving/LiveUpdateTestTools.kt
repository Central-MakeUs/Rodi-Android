package com.dororong.rodi.spike.driving

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.driving.DrivingSession
import com.dororong.rodi.core.domain.model.driving.DrivingSessionStatus
import com.dororong.rodi.core.domain.usecase.driving.ObserveLiveUpdateSettingsUseCase
import com.dororong.rodi.core.ui.permission.canPostPromotedNotifications
import com.dororong.rodi.feature.mypage.testmenu.TestMenuAction
import com.dororong.rodi.feature.mypage.testmenu.TestMenuSection
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface LiveUpdateTestEntryPoint {
    fun observeLiveUpdateSettings(): ObserveLiveUpdateSettingsUseCase
}

/**
 * 실제 주행 없이 운전 알림을 상태별로 띄우고, 실시간 업데이트로 승격되지 않을 때 어느 조건에서
 * 막혔는지 보여 준다. 알림은 운전 서비스와 같은 [DrivingNotificationFactory]로 만들어 모양이 같다.
 * 진행 중인 실제 주행 알림을 덮지 않도록 알림 ID만 다르게 쓴다.
 */
internal object LiveUpdateTestTools {
    private const val PREVIEW_NOTIFICATION_ID = DrivingNotificationFactory.NOTIFICATION_ID + 1
    private const val PREVIEW_PLANNED_DISTANCE_METERS = 5_000
    private const val SETTINGS_READ_TIMEOUT_MILLIS = 500L

    fun section(context: Context): TestMenuSection {
        val appContext = context.applicationContext
        return TestMenuSection(
            title = "라이브 업데이트",
            actions = listOf(
                TestMenuAction("연습 코스로 이동 중") { postOngoing(appContext, traveledRatio = 0.0) },
                TestMenuAction("코스 주행 중") { postOngoing(appContext, traveledRatio = 0.55) },
                TestMenuAction("코스 주행 중 - 방금 출발") { postOngoing(appContext, traveledRatio = 0.03) },
                TestMenuAction("코스 주행 완료") { postArrival(appContext) },
                TestMenuAction("테스트 알림 지우기") {
                    NotificationManagerCompat.from(appContext).cancel(PREVIEW_NOTIFICATION_ID)
                    null
                },
                TestMenuAction("진단 정보") { diagnostics(appContext) },
            ),
        )
    }

    private fun postOngoing(context: Context, traveledRatio: Double): String? {
        val session = previewSession()
        return post(
            context,
            DrivingNotificationFactory.ongoing(
                context = context,
                session = session,
                traveledDistanceMeters = PREVIEW_PLANNED_DISTANCE_METERS * traveledRatio,
                liveUpdateEnabled = isLiveUpdateEnabled(context),
            ),
        )
    }

    private fun postArrival(context: Context): String? {
        val now = System.currentTimeMillis()
        val session = previewSession().copy(arrivedAtEpochMillis = now, status = DrivingSessionStatus.ARRIVED)
        return post(context, DrivingNotificationFactory.arrival(context, session))
    }

    @SuppressLint("MissingPermission")
    private fun post(context: Context, notification: Notification): String? {
        DrivingNotificationFactory.createChannels(context)
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return "알림 권한이 꺼져 있어 알림을 띄울 수 없어요."
        manager.notify(PREVIEW_NOTIFICATION_ID, notification)
        return null
    }

    private fun diagnostics(context: Context): String = buildString {
        val sdk = Build.VERSION.SDK_INT
        val hasFullSdk = sdk >= Build.VERSION_CODES.BAKLAVA
        val fullSdkText = if (hasFullSdk) sdkFullText() else "$sdk"
        appendLine("[기기]")
        appendLine("${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Android ${Build.VERSION.RELEASE} (API $fullSdkText)")
        appendLine()
        val requested = isLiveUpdateEnabled(context)
        val osSupported = hasFullSdk && isAtLeastBaklava1()
        val userAllowed = context.canPostPromotedNotifications()
        val isSamsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
        val samsungNote = if (isSamsung) " (삼성은 참고용)" else ""
        appendLine("[승격 조건]")
        appendLine("요청: ${yesNo(requested)} (앱 설정 실시간 업데이트)")
        appendLine("알림 형태: ${yesNo(ongoingPromotable(context))}")
        appendLine("OS 36.1 이상: ${yesNo(osSupported)}$samsungNote")
        appendLine("사용자 허용: ${yesNo(userAllowed)}$samsungNote")
        appendLine("알림 권한: ${yesNo(NotificationManagerCompat.from(context).areNotificationsEnabled())}")
        if (!requested) appendLine("→ 설정 > 권한 설정 변경에서 실시간 업데이트를 켜 주세요.")
        if (!isSamsung && !osSupported) appendLine("→ 이 OS 버전은 실시간 업데이트 표시를 지원하지 않아요.")
        if (!isSamsung && !userAllowed) appendLine("→ 시스템 앱 설정에서 이 앱의 실시간 업데이트가 꺼져 있어요.")
        appendLine()
        appendLine("[표시 중인 테스트 알림]")
        appendLine("시스템 승격: ${previewPromotionText(context)}")
        if (isSamsung) {
            appendLine()
            // One UI 8.0은 API 36.0이어도 자체 구현으로 표시하고, 앱별 허용(allowOngoingActivity)으로 막는다.
            // 그 허용값은 앱에서 읽을 수 없어 adb `dumpsys notification`의 AppSettings로만 확인된다.
            appendLine("[참고] 삼성 One UI 8은 OS·사용자 허용 값과 무관하게 삼성이 허용한 앱만 표시해요. 개발자 옵션 '모든 앱의 실시간 알림'을 켜면 모든 앱이 표시돼요. 요청·알림 형태가 '예'인데 안 보이면 이 경우예요.")
        }
    }.trimEnd()

    private fun ongoingPromotable(context: Context): Boolean {
        val notification = DrivingNotificationFactory.ongoing(
            context = context,
            session = previewSession(),
            traveledDistanceMeters = PREVIEW_PLANNED_DISTANCE_METERS * 0.5,
            liveUpdateEnabled = true,
        )
        return NotificationCompat.hasPromotableCharacteristics(notification)
    }

    @SuppressLint("NewApi")
    private fun previewPromotionText(context: Context): String {
        val posted = context.getSystemService(NotificationManager::class.java)
            ?.activeNotifications
            ?.firstOrNull { it.id == PREVIEW_NOTIFICATION_ID }
            ?: return "알림 없음 (먼저 상태 버튼을 눌러 주세요)"
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA || !isAtLeastBaklava1()) return "지원 안 함"
        return yesNo(posted.notification.flags and Notification.FLAG_PROMOTED_ONGOING != 0)
    }

    @SuppressLint("NewApi")
    private fun isAtLeastBaklava1(): Boolean =
        Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1

    @SuppressLint("NewApi")
    private fun sdkFullText(): String {
        val full = Build.VERSION.SDK_INT_FULL
        return "${Build.getMajorSdkVersion(full)}.${Build.getMinorSdkVersion(full)}"
    }

    private fun isLiveUpdateEnabled(context: Context): Boolean {
        val observe = EntryPointAccessors
            .fromApplication(context, LiveUpdateTestEntryPoint::class.java)
            .observeLiveUpdateSettings()
        return runBlocking {
            withTimeoutOrNull(SETTINGS_READ_TIMEOUT_MILLIS) { observe().first() }?.isEnabled ?: true
        }
    }

    private fun previewSession() = DrivingSession(
        id = "live-update-preview",
        placeId = 0,
        placeName = "북악스카이웨이 드라이브",
        destination = GeoPoint(lat = 37.5925, lng = 126.9820),
        plannedDistanceMeters = PREVIEW_PLANNED_DISTANCE_METERS,
        startedAtEpochMillis = System.currentTimeMillis(),
        arrivedAtEpochMillis = null,
        traveledDistanceMeters = 0.0,
        status = DrivingSessionStatus.ACTIVE,
    )

    private fun yesNo(value: Boolean) = if (value) "예" else "아니오"
}
