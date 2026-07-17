package com.dororong.rodi.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.dororong.rodi.core.ui.components.RodiBottomNavigation
import com.dororong.rodi.core.ui.components.RodiBottomNavigationDestination
import com.dororong.rodi.feature.home.HomeScreen
import com.dororong.rodi.feature.mypage.MyPageScreen
import com.dororong.rodi.feature.mypage.drivinggoal.DrivingGoalScreen
import com.dororong.rodi.feature.mypage.savedcourses.SavedCoursesScreen
import com.dororong.rodi.feature.settings.SettingsScreen

@Composable
fun MainScreen() {
    val backStack = remember { mutableStateListOf<Any>(HomeRoute) }
    val currentRoute = backStack.lastOrNull()
    val bottomNavigationDestination = when (currentRoute) {
        HomeRoute -> RodiBottomNavigationDestination.Home
        MyPageRoute -> RodiBottomNavigationDestination.My
        else -> null
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            bottomNavigationDestination?.let { destination ->
                RodiBottomNavigation(
                    selectedDestination = destination,
                    onHomeClick = {
                        if (currentRoute != HomeRoute) {
                            backStack[backStack.lastIndex] = HomeRoute
                        }
                    },
                    onMyClick = {
                        if (currentRoute != MyPageRoute) {
                            backStack[backStack.lastIndex] = MyPageRoute
                        }
                    },
                )
            }
        },
    ) { contentPadding ->
        NavDisplay(
            modifier = Modifier
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding),
            backStack = backStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = { key ->
                when (key) {
                    HomeRoute -> NavEntry(key) {
                        HomeScreen(onNavigateSettings = { backStack.add(SettingsRoute) })
                    }
                    MyPageRoute -> NavEntry(key) {
                        MyPageScreen(
                            onSettingsClick = { backStack.add(SettingsRoute) },
                            onGoalClick = { backStack.add(DrivingGoalRoute) },
                            onSavedCoursesClick = { backStack.add(SavedCoursesRoute) },
                        )
                    }
                    DrivingGoalRoute -> NavEntry(key) {
                        DrivingGoalScreen(
                            onBack = { backStack.removeAt(backStack.lastIndex) },
                        )
                    }
                    SavedCoursesRoute -> NavEntry(key) {
                        SavedCoursesScreen(
                            onBack = { backStack.removeAt(backStack.lastIndex) },
                        )
                    }
                    SettingsRoute -> NavEntry(key) {
                        SettingsScreen(
                            onBack = {
                                if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                            },
                        )
                    }
                    else -> error("Unknown main route: $key")
                }
            },
        )
    }
}
