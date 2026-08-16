package com.dororong.rodi.spike.driving

import org.junit.Assert.assertEquals
import org.junit.Test

class DrivingNotificationStylePolicyTest {
    @Test
    fun `api thirty five uses standard ongoing notification`() {
        assertEquals(
            DrivingNotificationStyle.STANDARD,
            DrivingNotificationStylePolicy.forApi(35),
        )
    }

    @Test
    fun `api thirty six uses progress style`() {
        assertEquals(
            DrivingNotificationStyle.PROGRESS_STYLE,
            DrivingNotificationStylePolicy.forApi(36),
        )
    }

    @Test
    fun `api thirty five does not request live update promotion`() {
        assertEquals(false, DrivingNotificationStylePolicy.requestsPromotion(35))
    }

    @Test
    fun `api thirty six requests live update promotion`() {
        assertEquals(true, DrivingNotificationStylePolicy.requestsPromotion(36))
    }
}
