package com.dororong.rodi.spike.driving

import org.junit.Assert.assertEquals
import org.junit.Test

class DrivingNotificationStylePolicyTest {
    @Test
    fun apiThirtyFiveUsesStandardOngoingNotification() {
        assertEquals(
            DrivingNotificationStyle.STANDARD,
            DrivingNotificationStylePolicy.forApi(35),
        )
    }

    @Test
    fun apiThirtySixUsesProgressStyle() {
        assertEquals(
            DrivingNotificationStyle.PROGRESS_STYLE,
            DrivingNotificationStylePolicy.forApi(36),
        )
    }

    @Test
    fun apiThirtyFiveDoesNotRequestPromotion() {
        assertEquals(false, DrivingNotificationStylePolicy.requestsPromotion(35))
    }

    @Test
    fun apiThirtySixRequestsPromotion() {
        assertEquals(true, DrivingNotificationStylePolicy.requestsPromotion(36))
    }
}
