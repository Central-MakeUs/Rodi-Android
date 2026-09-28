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
    fun `Android 16 미만에서는 실시간 업데이트를 허용된 것으로 본다`() {
        assertTrue(application.canPostPromotedNotifications())
    }

    @Test
    @Config(sdk = [36])
    fun `Android 16에서는 알림 관리자의 응답을 따른다`() {
        // 승격 허용은 기기·사용자 설정에서 나온다. 기본값은 불가다.
        assertFalse(application.canPostPromotedNotifications())
    }
}
