package com.dororong.rodi.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object LoginRoute : NavKey

@Serializable
data object EntryRoute : NavKey

@Serializable
data object MainRoute : NavKey

@Serializable
data object HomeRoute : NavKey

@Serializable
data class SearchRoute(
    val latitude: Double,
    val longitude: Double,
) : NavKey

@Serializable
data object MyPageRoute : NavKey

@Serializable
data object DrivingGoalRoute : NavKey

@Serializable
data object SavedCoursesRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

internal fun NavKey.toClarityScreenName(): String? = when (this) {
    LoginRoute -> "Login"
    EntryRoute -> "Onboarding"
    MainRoute -> null
    HomeRoute -> "Home"
    is SearchRoute -> "Search"
    MyPageRoute -> "MyPage"
    DrivingGoalRoute -> "DrivingGoal"
    SavedCoursesRoute -> "SavedCourses"
    SettingsRoute -> "Settings"
    else -> null
}
