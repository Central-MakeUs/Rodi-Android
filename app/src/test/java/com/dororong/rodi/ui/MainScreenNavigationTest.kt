package com.dororong.rodi.ui

import androidx.navigation3.runtime.NavKey
import com.dororong.rodi.core.ui.components.RodiBottomNavigationDestination
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MainScreenNavigationTest {
    @Test
    fun `운전 목표 완료는 자기 화면만 한 번 닫는다`() {
        val backStack = mutableListOf<NavKey>(HomeRoute, MyPageRoute, DrivingGoalRoute)

        backStack.popDrivingGoal()
        backStack.popDrivingGoal()

        assertEquals(listOf<NavKey>(HomeRoute, MyPageRoute), backStack)
    }

    @Test
    fun `운전 목표 완료는 맨 위가 다른 화면이면 그대로 둔다`() {
        val backStack = mutableListOf<NavKey>(HomeRoute, DrivingGoalRoute, SettingsRoute)

        backStack.popDrivingGoal()

        assertEquals(listOf<NavKey>(HomeRoute, DrivingGoalRoute, SettingsRoute), backStack)
    }

    @Test
    fun `마이페이지는 홈을 대체하지 않고 쌓였다가 닫으면 홈으로 돌아간다`() {
        val backStack = mutableListOf<NavKey>(HomeRoute)

        backStack.pushMyPage()
        assertEquals(listOf<NavKey>(HomeRoute, MyPageRoute), backStack)

        backStack.popMyPage()
        assertEquals(listOf<NavKey>(HomeRoute), backStack)
    }

    @Test
    fun `마이페이지 닫기는 맨 위가 관계없는 화면이면 그대로 둔다`() {
        val backStack = mutableListOf<NavKey>(HomeRoute, SearchRoute(37.0, 127.0))

        backStack.popMyPage()

        assertEquals(listOf<NavKey>(HomeRoute, SearchRoute(37.0, 127.0)), backStack)
    }

    @Test
    fun `마이페이지에서 연 코스 등록은 닫으면 마이페이지로 돌아간다`() {
        val backStack = mutableListOf<NavKey>(HomeRoute, MyPageRoute)

        backStack.openCourseRegistration()
        assertEquals(listOf<NavKey>(HomeRoute, MyPageRoute, CourseRegistrationFlowRoute), backStack)

        backStack.popCourseRegistration()
        assertEquals(listOf<NavKey>(HomeRoute, MyPageRoute), backStack)
    }

    @Test
    fun `홈에서 연 코스 등록은 닫으면 홈으로 돌아간다`() {
        val backStack = mutableListOf<NavKey>(HomeRoute)

        backStack.openCourseRegistration()
        assertEquals(listOf<NavKey>(HomeRoute, CourseRegistrationFlowRoute), backStack)

        backStack.popCourseRegistration()
        assertEquals(listOf<NavKey>(HomeRoute), backStack)
    }

    @Test
    fun `코스 등록 화면을 중복으로 쌓지 않는다`() {
        val backStack = mutableListOf<NavKey>(HomeRoute, CourseRegistrationFlowRoute)

        backStack.openCourseRegistration()

        assertEquals(listOf<NavKey>(HomeRoute, CourseRegistrationFlowRoute), backStack)
    }

    @Test
    fun `코스 등록을 완료하면 홈으로 돌아간다`() {
        val backStack = mutableListOf<NavKey>(HomeRoute, MyPageRoute, CourseRegistrationFlowRoute)

        backStack.completeCourseRegistration()

        assertEquals(listOf<NavKey>(HomeRoute), backStack)
    }

    @Test
    fun `하단 내비게이션은 홈과 마이페이지에서 보이고 코스 등록 흐름에서는 숨긴다`() {
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
