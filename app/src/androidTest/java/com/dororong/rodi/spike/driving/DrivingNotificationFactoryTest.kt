package com.dororong.rodi.spike.driving

import android.app.Notification
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.TypedValue
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dororong.rodi.R
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.driving.DrivingSession
import com.dororong.rodi.core.domain.model.driving.DrivingSessionStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrivingNotificationFactoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val session = DrivingSession(
        id = "notification-icon-test",
        placeId = 0,
        placeName = "검증용 코스",
        destination = GeoPoint(lat = 37.5925, lng = 126.9820),
        plannedDistanceMeters = 5_000,
        startedAtEpochMillis = 1_700_000_000_000,
        arrivedAtEpochMillis = null,
        traveledDistanceMeters = 0.0,
        status = DrivingSessionStatus.ACTIVE,
    )

    @Test
    fun `이동 중과 주행 중 알림은 컬러 로고와 단색 작은 아이콘을 제공한다`() {
        listOf(0.0, 2_750.0).forEach { distance ->
            assertBrandIcons(DrivingNotificationFactory.ongoing(context, session, distance))
        }
    }

    @Test
    fun `연습 완료 알림도 컬러 로고와 단색 작은 아이콘을 제공한다`() {
        assertBrandIcons(DrivingNotificationFactory.arrival(context, session))
    }

    @Test
    fun `앱 아이콘은 단색 레이어가 없는 어댑티브 아이콘이다`() {
        assertEquals(R.mipmap.ic_launcher, context.applicationInfo.icon)
        val icon = context.applicationInfo.loadIcon(context.packageManager)
        assertTrue(icon is AdaptiveIconDrawable)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            assertNull((icon as AdaptiveIconDrawable).monochrome)
        }
    }

    @Test
    fun `로고 이미지 소스는 래스터 PNG로 해석되며 작은 아이콘은 흰색 알파 마스크다`() {
        listOf(
            R.drawable.ic_rodi_brand,
            R.drawable.ic_launcher_foreground,
            R.drawable.ic_notification,
        ).forEach { resource ->
            val value = TypedValue()
            context.resources.getValue(resource, value, true)
            assertTrue(value.string.toString().endsWith(".png"))
            assertTrue(ContextCompat.getDrawable(context, resource) is BitmapDrawable)
        }
        val smallIcon = requireNotNull(ContextCompat.getDrawable(context, R.drawable.ic_notification)).toBitmap()
        var visiblePixelCount = 0
        for (y in 0 until smallIcon.height) {
            for (x in 0 until smallIcon.width) {
                val pixel = smallIcon.getPixel(x, y)
                if (Color.alpha(pixel) > 0) {
                    visiblePixelCount++
                    assertEquals(255, Color.red(pixel))
                    assertEquals(255, Color.green(pixel))
                    assertEquals(255, Color.blue(pixel))
                }
            }
        }
        assertTrue(visiblePixelCount > 0)
    }

    @Test
    fun `알림 컬러 로고는 모서리가 투명한 원형이며 가장자리 가까이까지 채워진다`() {
        val source = BitmapFactory.decodeResource(context.resources, R.drawable.ic_rodi_brand)
        val notifications = listOf(
            DrivingNotificationFactory.ongoing(context, session, 0.0),
            DrivingNotificationFactory.ongoing(context, session, 2_750.0),
            DrivingNotificationFactory.arrival(context, session),
        )
        notifications.flatMap { listOf(it, requireNotNull(it.publicVersion)) }.forEach { notification ->
            val bitmap = requireNotNull(notification.getLargeIcon().loadDrawable(context)).toBitmap()
            assertEquals(source.width, bitmap.width)
            assertEquals(source.height, bitmap.height)
            listOf(0 to 0, bitmap.width - 1 to 0, 0 to bitmap.height - 1, bitmap.width - 1 to bitmap.height - 1)
                .forEach { (x, y) -> assertEquals(0, Color.alpha(bitmap.getPixel(x, y))) }
            listOf(bitmap.width / 2 to EDGE_INSET, EDGE_INSET to bitmap.height / 2)
                .forEach { (x, y) -> assertTrue(Color.alpha(bitmap.getPixel(x, y)) >= 250) }
            val center = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
            assertEquals(255, Color.alpha(center))
            assertTrue(Color.blue(center) > Color.red(center) + 30)
        }
    }

    @Test
    fun `최초 알림과 거리 갱신은 공개 잠금 내용과 지속 플래그를 유지한다`() {
        listOf(true, false).forEach { liveUpdates ->
            listOf(0.0, 54.0, 2_750.0).forEach { distance ->
                val notification = DrivingNotificationFactory.ongoing(context, session, distance, liveUpdates)

                assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
                assertFalse(notification.flags and Notification.FLAG_AUTO_CANCEL != 0)
                assertPublicContent(notification)
                assertFalse(notification.extras.getCharSequence(Notification.EXTRA_TITLE).isNullOrBlank())
                assertFalse(notification.extras.getCharSequence(Notification.EXTRA_TEXT).isNullOrBlank())
                if (distance > 0) {
                    assertEquals(
                        notification.extras.getInt(Notification.EXTRA_PROGRESS),
                        notification.publicVersion.extras.getInt(Notification.EXTRA_PROGRESS),
                    )
                }
                // 진행 스타일 경로는 EXTRA_PROGRESS를 채우지 않으므로 기본 setProgress 경로에서만 값을 본다.
                if (!liveUpdates && distance == 2_750.0) {
                    assertEquals(55, notification.extras.getInt(Notification.EXTRA_PROGRESS))
                }
            }
        }
    }

    @Test
    fun `완료 알림은 공개 내용을 제공하며 선택 후 닫히는 일반 알림이다`() {
        val notification = DrivingNotificationFactory.arrival(context, session)

        assertPublicContent(notification)
        assertFalse(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertTrue(notification.flags and Notification.FLAG_AUTO_CANCEL != 0)
    }

    private fun assertPublicContent(notification: Notification) {
        val publicContent = requireNotNull(notification.publicVersion)
        assertEquals(Notification.VISIBILITY_PUBLIC, notification.visibility)
        assertEquals(Notification.VISIBILITY_PUBLIC, publicContent.visibility)
        assertBrandIcons(publicContent)
        listOf(Notification.EXTRA_TITLE, Notification.EXTRA_TEXT, Notification.EXTRA_TEMPLATE).forEach { key ->
            assertEquals(notification.extras.get(key), publicContent.extras.get(key))
        }
        assertEquals(notification.contentIntent, publicContent.contentIntent)
        assertEquals(notification.actions.size, publicContent.actions.size)
        assertEquals(notification.flags, publicContent.flags)
        assertEquals(null, publicContent.publicVersion)
    }

    private fun assertBrandIcons(notification: Notification) {
        assertEquals(R.drawable.ic_notification, notification.smallIcon.resId)
        notification.actions.forEach { assertEquals(R.drawable.ic_notification, it.getIcon().resId) }
        val icon = notification.getLargeIcon()
        assertNotNull(icon)
        val drawable = requireNotNull(icon.loadDrawable(context))
        val bitmap = drawable.toBitmap()
        val colors = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(colors, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        assertTrue(colors.any { Color.alpha(it) > 0 && Color.blue(it) > Color.red(it) + 30 })
    }

    private companion object {
        /** 원의 가장자리 한 점은 안티앨리어싱으로 반투명하므로 그보다 조금 안쪽을 본다. */
        const val EDGE_INSET = 3
    }
}
