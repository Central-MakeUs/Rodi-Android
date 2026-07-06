package com.dororong.rodi.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() {
        baselineProfileRule.collect(packageName = targetPackageName) {
            pressHome()
            startActivityAndWait()
        }
    }

    private companion object {
        val targetPackageName: String
            get() = InstrumentationRegistry.getInstrumentation().targetContext.packageName
    }
}
