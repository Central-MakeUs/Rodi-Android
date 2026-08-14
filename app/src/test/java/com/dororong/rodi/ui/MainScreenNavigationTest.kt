package com.dororong.rodi.ui

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Test

class MainScreenNavigationTest {
    @Test
    fun `my page is pushed without replacing home and popped back to home`() {
        val backStack = mutableListOf<NavKey>(HomeRoute)

        backStack.pushMyPage()
        assertEquals(listOf<NavKey>(HomeRoute, MyPageRoute), backStack)

        backStack.popMyPage()
        assertEquals(listOf<NavKey>(HomeRoute), backStack)
    }

    @Test
    fun `pop my page leaves unrelated top route unchanged`() {
        val backStack = mutableListOf<NavKey>(HomeRoute, SearchRoute(37.0, 127.0))

        backStack.popMyPage()

        assertEquals(listOf<NavKey>(HomeRoute, SearchRoute(37.0, 127.0)), backStack)
    }
}
