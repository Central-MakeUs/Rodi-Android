package com.dororong.rodi.spike.driving

import android.os.Build
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DrivingNotificationStylePolicyTest {
    @Test
    fun `android 16 uses the progress style and asks for promotion`() {
        assertEquals(
            DrivingNotificationStyle.PROGRESS_STYLE,
            DrivingNotificationStylePolicy.forApi(Build.VERSION_CODES.BAKLAVA),
        )
        assertTrue(DrivingNotificationStylePolicy.requestsPromotion(Build.VERSION_CODES.BAKLAVA))
    }

    @Test
    fun `below android 16 stays a standard notification`() {
        assertEquals(
            DrivingNotificationStyle.STANDARD,
            DrivingNotificationStylePolicy.forApi(Build.VERSION_CODES.VANILLA_ICE_CREAM),
        )
        assertFalse(DrivingNotificationStylePolicy.requestsPromotion(Build.VERSION_CODES.VANILLA_ICE_CREAM))
    }

    @Test
    fun `turning the app setting off drops the progress style and the promotion request`() {
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
