package com.dororong.rodi.core.ui.permission

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class LiveUpdatePermissionTest {
    private lateinit var application: Application

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        shadowOf(application).checkActivities(true)
    }

    @Test
    @Config(sdk = [35])
    fun `live updates are treated as allowed below Android 16`() {
        assertTrue(application.canPostPromotedNotifications())
    }

    @Test
    @Config(sdk = [36])
    fun `opens the promotion settings screen when the system provides it`() {
        registerActivity(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS)

        application.openPromotedNotificationSettings()

        val started = shadowOf(application).nextStartedActivity
        assertEquals(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS, started.action)
        assertEquals(application.packageName, started.getStringExtra(Settings.EXTRA_APP_PACKAGE))
    }

    @Test
    @Config(sdk = [36])
    fun `falls back to app notification settings when promotion settings are missing`() {
        registerActivity(Settings.ACTION_APP_NOTIFICATION_SETTINGS)

        application.openPromotedNotificationSettings()

        assertEquals(
            Settings.ACTION_APP_NOTIFICATION_SETTINGS,
            shadowOf(application).nextStartedActivity.action,
        )
    }

    @Test
    @Config(sdk = [35])
    fun `skips the promotion settings screen below Android 16`() {
        registerActivity(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS)
        registerActivity(Settings.ACTION_APP_NOTIFICATION_SETTINGS)

        application.openPromotedNotificationSettings()

        assertEquals(
            Settings.ACTION_APP_NOTIFICATION_SETTINGS,
            shadowOf(application).nextStartedActivity.action,
        )
    }

    private fun registerActivity(action: String) {
        val component = ComponentName("com.android.settings", action)
        val packageManager = shadowOf(application.packageManager)
        packageManager.addActivityIfNotPresent(component)
        packageManager.addIntentFilterForActivity(component, IntentFilter(action).apply { addCategory(Intent.CATEGORY_DEFAULT) })
    }
}
