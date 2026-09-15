package com.dororong.rodi.ui

import androidx.navigation3.runtime.NavKey
import com.dororong.rodi.core.ui.components.RodiBottomNavigationDestination
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

    @Test
    fun `registration route is pushed from my page and popped back to the source`() {
        val backStack = mutableListOf<NavKey>(HomeRoute, MyPageRoute)

        backStack.openCourseRegistration()
        assertEquals(listOf<NavKey>(HomeRoute, MyPageRoute, CourseRegistrationFlowRoute), backStack)

        backStack.popCourseRegistration()
        assertEquals(listOf<NavKey>(HomeRoute, MyPageRoute), backStack)
    }

    @Test
    fun `registration route is pushed from home and popped back to home`() {
        val backStack = mutableListOf<NavKey>(HomeRoute)

        backStack.openCourseRegistration()
        assertEquals(listOf<NavKey>(HomeRoute, CourseRegistrationFlowRoute), backStack)

        backStack.popCourseRegistration()
        assertEquals(listOf<NavKey>(HomeRoute), backStack)
    }

    @Test
    fun `registration route is not duplicated`() {
        val backStack = mutableListOf<NavKey>(HomeRoute, CourseRegistrationFlowRoute)

        backStack.openCourseRegistration()

        assertEquals(listOf<NavKey>(HomeRoute, CourseRegistrationFlowRoute), backStack)
    }

    @Test
    fun `completed registration returns to home`() {
        val backStack = mutableListOf<NavKey>(HomeRoute, MyPageRoute, CourseRegistrationFlowRoute)

        backStack.completeCourseRegistration()

        assertEquals(listOf<NavKey>(HomeRoute), backStack)
    }

    @Test
    fun `bottom navigation is owned by home and my page but hidden in registration flow`() {
        assertEquals(true, HomeRoute.shouldShowMainBottomNavigation())
        assertEquals(true, MyPageRoute.shouldShowMainBottomNavigation())
        assertEquals(false, CourseRegistrationFlowRoute.shouldShowMainBottomNavigation())
        assertEquals(RodiBottomNavigationDestination.Home, HomeRoute.toBottomNavigationDestination())
        assertEquals(RodiBottomNavigationDestination.My, MyPageRoute.toBottomNavigationDestination())
        assertEquals(
            RodiBottomNavigationDestination.Register,
            CourseRegistrationFlowRoute.toBottomNavigationDestination(),
        )
        assertEquals(
            RodiBottomNavigationDestination.Register,
            HomeRoute.toMainBottomNavigationDestination(showResumeDialog = true),
        )
        assertEquals(
            RodiBottomNavigationDestination.Home,
            HomeRoute.toMainBottomNavigationDestination(showResumeDialog = false),
        )
    }
}
