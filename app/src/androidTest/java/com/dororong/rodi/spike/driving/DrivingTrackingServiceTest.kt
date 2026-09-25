package com.dororong.rodi.spike.driving

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceType
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrivingTrackingServiceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext

    @Before
    fun grantTrackingPermissions() {
        val permissions = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissions.forEach { instrumentation.uiAutomation.grantRuntimePermission(context.packageName, it) }
    }

    @After
    fun stopTracking() {
        instrumentation.runOnMainSync { DrivingTrackingController.stop(context) }
        awaitCondition { ongoingSessionStartedAt() == null }
    }

    @Test
    fun stopFollowedImmediatelyByStartTracksTheNewPlace() {
        instrumentation.runOnMainSync { DrivingTrackingController.start(context, place(1L, "첫 번째 장소")).getOrThrow() }
        assertTrue(awaitCondition { ongoingSessionStartedAt() != null })
        val firstSessionStartedAt = requireNotNull(ongoingSessionStartedAt())

        instrumentation.runOnMainSync {
            DrivingTrackingController.stop(context)
            DrivingTrackingController.start(context, place(2L, "두 번째 장소")).getOrThrow()
        }

        assertTrue(
            "다른 장소로 바꾼 뒤 새 세션의 추적 알림이 떠 있어야 한다. 현재 세션 시작=${ongoingSessionStartedAt()}",
            awaitCondition(stableForMillis = 1_500) {
                ongoingSessionStartedAt()?.let { it > firstSessionStartedAt } == true
            },
        )
    }

    @Test
    fun startForAnotherPlaceWithoutStopReplacesTheSession() {
        instrumentation.runOnMainSync { DrivingTrackingController.start(context, place(1L, "첫 번째 장소")).getOrThrow() }
        assertTrue(awaitCondition { ongoingSessionStartedAt() != null })
        val firstSessionStartedAt = requireNotNull(ongoingSessionStartedAt())

        instrumentation.runOnMainSync { DrivingTrackingController.start(context, place(2L, "두 번째 장소")).getOrThrow() }

        assertTrue(
            awaitCondition(stableForMillis = 1_500) {
                ongoingSessionStartedAt()?.let { it > firstSessionStartedAt } == true
            },
        )
    }

    @Test
    fun repeatedStopEndsTrackingAndRemovesTheNotification() {
        instrumentation.runOnMainSync { DrivingTrackingController.start(context, place(1L, "첫 번째 장소")).getOrThrow() }
        assertTrue(awaitCondition { ongoingSessionStartedAt() != null })

        instrumentation.runOnMainSync {
            DrivingTrackingController.stop(context)
            DrivingTrackingController.stop(context)
        }

        assertTrue(awaitCondition(stableForMillis = 1_000) { ongoingSessionStartedAt() == null })
    }

    /** 추적 알림은 세션 시작 시각을 when으로 쓴다. 세션이 바뀌면 이 값도 바뀐다. */
    private fun ongoingSessionStartedAt(): Long? = context.getSystemService(NotificationManager::class.java)
        .activeNotifications
        .firstOrNull { it.id == DrivingNotificationFactory.NOTIFICATION_ID && it.isOngoing }
        ?.notification
        ?.`when`

    private fun awaitCondition(
        timeoutMillis: Long = 5_000,
        stableForMillis: Long = 0,
        condition: () -> Boolean,
    ): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        var satisfiedSince: Long? = null
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) {
                val since = satisfiedSince ?: SystemClock.uptimeMillis().also { satisfiedSince = it }
                if (SystemClock.uptimeMillis() - since >= stableForMillis) return true
            } else {
                satisfiedSince = null
            }
            SystemClock.sleep(POLL_INTERVAL_MILLIS)
        }
        return false
    }

    private fun place(id: Long, name: String) = PlaceDetail(
        id = id,
        type = PlaceType.PARKING,
        name = name,
        address = "",
        point = GeoPoint(37.5, 127.0),
        practiceTypes = emptyList(),
        bookmarkCount = 0,
        isBookmarked = false,
        course = null,
        parking = null,
    )

    private companion object {
        const val POLL_INTERVAL_MILLIS = 100L
    }
}
