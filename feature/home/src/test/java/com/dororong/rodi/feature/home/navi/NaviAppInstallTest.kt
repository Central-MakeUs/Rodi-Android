package com.dororong.rodi.feature.home.navi

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class NaviAppInstallTest {
    private lateinit var application: Application

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        shadowOf(application).checkActivities(true)
    }

    @Test
    fun `opens the market app when it can handle the store link`() {
        registerViewHandler(scheme = "market")
        registerViewHandler(scheme = "https")

        application.openPlayStore("com.locnall.KimGiSa")

        assertEquals(
            "market://details?id=com.locnall.KimGiSa",
            shadowOf(application).nextStartedActivity.dataString,
        )
    }

    @Test
    fun `falls back to the web store when no market app is installed`() {
        registerViewHandler(scheme = "https")

        application.openPlayStore("net.daum.android.map")

        assertEquals(
            "https://play.google.com/store/apps/details?id=net.daum.android.map",
            shadowOf(application).nextStartedActivity.dataString,
        )
    }

    @Test
    fun `reports whether a package is installed`() {
        shadowOf(application.packageManager).installPackage(
            PackageInfo().apply { packageName = "net.daum.android.map" },
        )

        assertTrue(application.isPackageInstalled("net.daum.android.map"))
        assertFalse(application.isPackageInstalled("com.locnall.KimGiSa"))
    }

    private fun registerViewHandler(scheme: String) {
        val component = ComponentName("com.example.$scheme", "$scheme.Handler")
        val packageManager = shadowOf(application.packageManager)
        packageManager.addActivityIfNotPresent(component)
        packageManager.addIntentFilterForActivity(
            component,
            IntentFilter(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                addDataScheme(scheme)
            },
        )
    }
}
