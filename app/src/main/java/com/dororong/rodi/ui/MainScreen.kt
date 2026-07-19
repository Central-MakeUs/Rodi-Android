package com.dororong.rodi.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.LocalActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.dororong.rodi.BuildConfig
import com.dororong.rodi.core.ui.components.RodiBottomNavigation
import com.dororong.rodi.core.ui.components.RodiBottomNavigationDestination
import com.dororong.rodi.feature.home.HomeIntent
import com.dororong.rodi.feature.home.HomeScreen
import com.dororong.rodi.feature.home.HomeViewModel
import com.dororong.rodi.feature.auth.KakaoLoginManagerEntryPoint
import com.dororong.rodi.feature.mypage.MyPageScreen
import com.dororong.rodi.feature.mypage.drivinggoal.DrivingGoalScreen
import com.dororong.rodi.feature.mypage.savedcourses.SavedCoursesScreen
import com.dororong.rodi.feature.settings.SettingsScreen
import dagger.hilt.android.EntryPointAccessors

@Composable
fun MainScreen(
    onSessionEnded: () -> Unit,
) {
    val backStack = rememberNavBackStack(HomeRoute)
    val currentRoute = backStack.lastOrNull()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val activity = LocalActivity.current
    val kakaoLoginManager = remember(activity) {
        activity?.let {
            EntryPointAccessors.fromActivity(it, KakaoLoginManagerEntryPoint::class.java)
                .kakaoLoginManager()
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavDisplay(
            modifier = Modifier.fillMaxSize(),
            backStack = backStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = { key ->
                when (key) {
                    HomeRoute -> NavEntry(key) {
                        HomeScreen(
                            onMyPageClick = {
                                backStack[backStack.lastIndex] = MyPageRoute
                            },
                            onRequestKakaoLogin = { onSuccess, onFailure ->
                                kakaoLoginManager?.login(onSuccess, onFailure)
                                    ?: onFailure("로그인을 진행할 수 없습니다. 다시 시도해주세요.")
                            },
                            vm = homeViewModel,
                        )
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
                            appVersion = BuildConfig.VERSION_NAME,
                            onSessionEnded = onSessionEnded,
                        )
                    }
                    else -> error("Unknown main route: $key")
                }
            },
        )
        if (currentRoute == HomeRoute || currentRoute == MyPageRoute) {
            val selectedDestination = if (currentRoute == HomeRoute) {
                RodiBottomNavigationDestination.Home
            } else {
                RodiBottomNavigationDestination.My
            }
                RodiBottomNavigation(
                    selectedDestination = selectedDestination,
                    onHomeClick = {
                        if (currentRoute == HomeRoute) {
                            homeViewModel.onIntent(HomeIntent.OnListOpen)
                        } else {
                            backStack[backStack.lastIndex] = HomeRoute
                        }
                    },
                    onMyClick = {
                        if (currentRoute == HomeRoute) {
                            homeViewModel.onIntent(HomeIntent.OnMyClick)
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
        }
    }
}
