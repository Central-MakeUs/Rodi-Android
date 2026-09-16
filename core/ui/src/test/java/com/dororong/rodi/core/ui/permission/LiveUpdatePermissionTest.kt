package com.dororong.rodi.core.ui.permission

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class LiveUpdatePermissionTest {
    private val application: Application = ApplicationProvider.getApplicationContext()

    @Test
    @Config(sdk = [35])
    fun `live updates are treated as allowed below Android 16`() {
        assertTrue(application.canPostPromotedNotifications())
    }

    @Test
    @Config(sdk = [36])
    fun `android 16 answers from the notification manager`() {
        // 승격 허용은 기기·사용자 설정에서 나온다. 기본값은 불가다.
        assertFalse(application.canPostPromotedNotifications())
    }
}
