package com.dororong.rodi.spike.driving

import android.os.Build
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DrivingNotificationStylePolicyTest {
    @Test
    fun `Android 16에서는 진행 스타일을 쓰고 승격을 요청한다`() {
        assertEquals(
            DrivingNotificationStyle.PROGRESS_STYLE,
            DrivingNotificationStylePolicy.forApi(Build.VERSION_CODES.BAKLAVA),
        )
        assertTrue(DrivingNotificationStylePolicy.requestsPromotion(Build.VERSION_CODES.BAKLAVA))
    }

    @Test
    fun `Android 16 미만에서는 일반 알림으로 표시한다`() {
        assertEquals(
            DrivingNotificationStyle.STANDARD,
            DrivingNotificationStylePolicy.forApi(Build.VERSION_CODES.VANILLA_ICE_CREAM),
        )
        assertFalse(DrivingNotificationStylePolicy.requestsPromotion(Build.VERSION_CODES.VANILLA_ICE_CREAM))
    }

    @Test
    fun `앱 설정을 끄면 진행 스타일과 승격 요청을 쓰지 않는다`() {
        assertEquals(
            DrivingNotificationStyle.STANDARD,
            DrivingNotificationStylePolicy.forApi(Build.VERSION_CODES.BAKLAVA, liveUpdateEnabled = false),
        )
        assertFalse(
            DrivingNotificationStylePolicy.requestsPromotion(
                Build.VERSION_CODES.BAKLAVA,
                liveUpdateEnabled = false,
            ),
        )
    }
}
