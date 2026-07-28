package com.dororong.rodi.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import com.dororong.rodi.feature.home.HomeDetailOrigin
import com.dororong.rodi.feature.home.HomeScreen
import com.dororong.rodi.feature.home.HomeViewModel
import com.dororong.rodi.feature.home.SearchScreen
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.feature.auth.KakaoLoginManagerEntryPoint
import com.dororong.rodi.feature.mypage.MyPageScreen
import com.dororong.rodi.feature.mypage.drivinggoal.DrivingGoalScreen
import com.dororong.rodi.feature.mypage.savedcourses.SavedCoursesScreen
import com.dororong.rodi.feature.settings.SettingsScreen
import dagger.hilt.android.EntryPointAccessors

@Composable
fun MainScreen(
    onSessionEnded: () -> Unit,
    onGuestSignUp: () -> Unit,
) {
    val backStack = rememberNavBackStack(HomeRoute)
    val currentRoute = backStack.lastOrNull()
    val currentRouteState = rememberUpdatedState(currentRoute)
    val homeViewModel: HomeViewModel = hiltViewModel()
    val activity = LocalActivity.current
    val kakaoLoginManager = remember(activity) {
        activity?.let {
            EntryPointAccessors.fromActivity(it, KakaoLoginManagerEntryPoint::class.java)
                .kakaoLoginManager()
        }
    }
    val bottomNavigation = remember {
        movableContentOf {
            val route = currentRouteState.value
            RodiBottomNavigation(
                selectedDestination = if (route == MyPageRoute) {
                    RodiBottomNavigationDestination.My
                } else {
                    RodiBottomNavigationDestination.Home
                },
                onHomeClick = {
                    if (route == HomeRoute) {
                        homeViewModel.onIntent(HomeIntent.OnListOpen)
                    } else {
                        backStack[backStack.lastIndex] = HomeRoute
                    }
                },
                onMyClick = {
                    if (route == HomeRoute) homeViewModel.onIntent(HomeIntent.OnMyClick)
                },
            )
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
                            onSearchClick = { origin ->
                                backStack.add(SearchRoute(origin.lat, origin.lng))
                            },
                            onGuestSignUp = onGuestSignUp,
                            onRequestKakaoLogin = { onSuccess, onFailure ->
                                kakaoLoginManager?.login(onSuccess, onFailure)
                                    ?: onFailure("로그인을 진행할 수 없습니다. 다시 시도해주세요.")
                            },
                            bottomNavigation = {
                                if (currentRouteState.value == HomeRoute) bottomNavigation()
                            },
                            vm = homeViewModel,
                        )
                    }
                    is SearchRoute -> NavEntry(key) {
                        SearchScreen(
                            origin = GeoPoint(key.latitude, key.longitude),
                            onBack = {
                                if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                            },
                            onPlaceClick = { placeId ->
                                homeViewModel.onIntent(HomeIntent.OnPlaceClick(placeId, HomeDetailOrigin.Map))
                                if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                            },
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
                            onPlaceClick = { placeId ->
                                homeViewModel.onIntent(HomeIntent.OnPlaceClick(placeId, HomeDetailOrigin.Map))
                                backStack.clear()
                                backStack.add(HomeRoute)
                            },
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
        if (currentRoute == MyPageRoute) {
            Box(Modifier.align(Alignment.BottomCenter)) { bottomNavigation() }
        }
    }
}
