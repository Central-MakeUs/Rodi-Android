package com.dororong.rodi.feature.home.navi

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

class KakaoNaviLauncherTest {
    private val originalLocale = Locale.getDefault()

    @AfterEach
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `toNaviCoordinate always formats with dot decimal separator`() {
        Locale.setDefault(Locale.GERMANY)

        val result = 126.922.toNaviCoordinate()

        assertEquals("126.922000", result)
    }
}
