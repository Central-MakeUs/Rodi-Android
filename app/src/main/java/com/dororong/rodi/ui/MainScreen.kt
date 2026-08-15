package com.dororong.rodi.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.LocalActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.microsoft.clarity.Clarity
import com.dororong.rodi.BuildConfig
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.ui.components.RodiBottomNavigation
import com.dororong.rodi.core.ui.components.RodiBottomNavigationDestination
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarData
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHost
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHostState
import com.dororong.rodi.spike.driving.DrivingTrackingController
import com.dororong.rodi.feature.home.HomeIntent
import com.dororong.rodi.feature.home.HomeDetailOrigin
import com.dororong.rodi.feature.home.HomeScreen
import com.dororong.rodi.feature.home.HomeViewModel
import com.dororong.rodi.feature.home.SearchScreen
import com.dororong.rodi.feature.auth.KakaoLoginManagerEntryPoint
import com.dororong.rodi.feature.course.registration.CourseRegistrationFlow
import com.dororong.rodi.feature.course.registration.CourseRegistrationLoadingIndicator
import com.dororong.rodi.feature.course.registration.CourseRegistrationViewModel
import com.dororong.rodi.feature.mypage.MyPageScreen
import com.dororong.rodi.feature.mypage.drivinggoal.DrivingGoalScreen
import com.dororong.rodi.feature.mypage.savedcourses.SavedCoursesScreen
import com.dororong.rodi.feature.mypage.practicerecords.PracticeRecordsScreen
import com.dororong.rodi.feature.mypage.myposts.MyPostsScreen
import com.dororong.rodi.feature.home.review.ReviewWriteScreen
import com.dororong.rodi.feature.home.review.notvisited.PracticeSkipReasonScreen
import com.dororong.rodi.feature.settings.SettingsScreen
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    onSessionEnded: () -> Unit,
    onGuestSignUp: () -> Unit,
    onLoginRequired: () -> Unit = {},
    openCourseRegistrationOnStart: Boolean = false,
    onCourseRegistrationOpened: () -> Unit = {},
    courseRegistrationEntryModeOnStart: CourseRegistrationEntryMode = CourseRegistrationEntryMode.Normal,
    onCourseRegistrationLoginRequired: ((CourseRegistrationEntryMode) -> Unit)? = null,
) {
    val backStack = rememberNavBackStack(HomeRoute)
    val currentRoute = backStack.lastOrNull()
    val courseRegistrationEntryViewModel: CourseRegistrationEntryViewModel = hiltViewModel()
    val entryState by courseRegistrationEntryViewModel.state.collectAsStateWithLifecycle()
    var pendingCourseRegistrationPreflight by remember { mutableStateOf(false) }
    var showResumeDialog by rememberSaveable { mutableStateOf(false) }
    var courseRegistrationEntryMode by remember { mutableStateOf(CourseRegistrationEntryMode.Normal) }
    val coroutineScope = rememberCoroutineScope()
    val courseRegistrationSnackbarHostState = remember { RodiSnackbarHostState() }

    fun openCourseRegistration(mode: CourseRegistrationEntryMode) {
        showResumeDialog = false
        courseRegistrationEntryMode = mode
        backStack.openCourseRegistration()
        onCourseRegistrationOpened()
    }

    fun requestCourseRegistration() {
        if (backStack.lastOrNull() != HomeRoute) {
            backStack.clear()
            backStack.add(HomeRoute)
        }
        pendingCourseRegistrationPreflight = true
    }

    LaunchedEffect(openCourseRegistrationOnStart, courseRegistrationEntryModeOnStart) {
        if (openCourseRegistrationOnStart) {
            courseRegistrationEntryMode = courseRegistrationEntryModeOnStart
            requestCourseRegistration()
        }
    }
    LaunchedEffect(pendingCourseRegistrationPreflight, entryState) {
        if (!pendingCourseRegistrationPreflight) return@LaunchedEffect
        val readyState = entryState as? CourseRegistrationEntryState.Ready ?: return@LaunchedEffect
        pendingCourseRegistrationPreflight = false
        when (readyState.draft.courseRegistrationPreflightDecision()) {
            CourseRegistrationPreflightDecision.OpenImmediately -> {
                openCourseRegistration(CourseRegistrationEntryMode.Normal)
            }
            CourseRegistrationPreflightDecision.ShowResumeDialog -> {
                courseRegistrationEntryMode = CourseRegistrationEntryMode.Normal
                showResumeDialog = true
            }
        }
    }
    LaunchedEffect(currentRoute) {
        currentRoute?.toClarityScreenName()?.let(Clarity::setCurrentScreenName)
    }
    val currentRouteState = rememberUpdatedState(currentRoute)
    val homeViewModel: HomeViewModel = hiltViewModel()
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = LocalActivity.current
    val kakaoLoginManager = remember(activity) {
        activity?.let {
            EntryPointAccessors.fromActivity(it, KakaoLoginManagerEntryPoint::class.java)
                .kakaoLoginManager()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                homeViewModel.onIntent(HomeIntent.OnAppResumed)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val bottomNavigation = remember {
        movableContentOf {
            val route = currentRouteState.value
            RodiBottomNavigation(
                selectedDestination = route.toMainBottomNavigationDestination(showResumeDialog),
                onHomeClick = {
                    if (route == HomeRoute) {
                        homeViewModel.onIntent(HomeIntent.OnListOpen)
                    } else {
                        backStack.popMyPage()
                    }
                },
                onRegisterClick = {
                    if (!showResumeDialog) {
                        when (route) {
                            HomeRoute -> homeViewModel.onIntent(HomeIntent.OnRegisterClick)
                            MyPageRoute -> requestCourseRegistration()
                            else -> Unit
                        }
                    }
                },
                onMyClick = {
                    if (route == HomeRoute) homeViewModel.onIntent(HomeIntent.OnMyClick)
                },
            )
        }
    }

    BackHandler(enabled = currentRoute == MyPageRoute) {
        backStack.popMyPage()
    }

    Box(Modifier.fillMaxSize()) {
        NavDisplay(
            modifier = Modifier.fillMaxSize(),
            backStack = backStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            transitionSpec = { ROUTE_FADE },
            popTransitionSpec = { ROUTE_FADE },
            predictivePopTransitionSpec = { ROUTE_FADE },
            entryProvider = { key ->
                when (key) {
                    HomeRoute -> NavEntry(key) {
                        HomeScreen(
                            onMyPageClick = {
                                backStack.pushMyPage()
                            },
                            onCourseRegistrationClick = { requestCourseRegistration() },
                            onSearchClick = { origin ->
                                backStack.add(SearchRoute(origin.lat, origin.lng))
                            },
                            onGuestSignUp = onGuestSignUp,
                            onPracticeSkipReasonClick = { practiceId ->
                                backStack.add(PracticeSkipReasonRoute(practiceId))
                            },
                            onRequestKakaoLogin = { onSuccess, onFailure ->
                                kakaoLoginManager?.login(onSuccess, onFailure)
                                    ?: onFailure("로그인을 진행할 수 없습니다. 다시 시도해주세요.")
                            },
                            onStartDriving = { place ->
                                activity?.let { DrivingTrackingController.start(it, place) }
                                    ?: Result.failure(
                                        IllegalStateException(
                                            "운전 상태 추적을 시작할 수 없어요. 다시 시도해 주세요.",
                                        ),
                                    )
                            },
                            onStopDriving = {
                                activity?.let { DrivingTrackingController.stop(it) }
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
                            onRegionClick = { region, initialPlaces ->
                                homeViewModel.onIntent(HomeIntent.OnRegionSearch(region, initialPlaces))
                                if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                            },
                        )
                    }
                    MyPageRoute -> NavEntry(key) {
                        MyPageScreen(
                            onSettingsClick = { backStack.add(SettingsRoute) },
                            onGoalClick = { backStack.add(DrivingGoalRoute) },
                            onSavedCoursesClick = { backStack.add(SavedCoursesRoute) },
                            onPracticeRecordsClick = { backStack.add(PracticeRecordsRoute) },
                            onMyPostsClick = { backStack.add(MyPostsRoute) },
                            onWriteReviewClick = { placeId, placeName ->
                                backStack.add(ReviewWriteRoute(placeId, placeName))
                            },
                            isDebugBuild = BuildConfig.DEBUG,
                            onSessionEnded = onSessionEnded,
                        )
                    }
                    CourseRegistrationFlowRoute -> NavEntry(key) {
                        val registrationViewModel: CourseRegistrationViewModel = hiltViewModel()
                        var registrationEntryReady by remember { mutableStateOf(false) }
                        LaunchedEffect(registrationViewModel, courseRegistrationEntryMode) {
                            val restoredState = registrationViewModel.state.first {
                                it.isDraftRestored || it.isAuthResolved
                            }
                            if (restoredState.isAuthResolved && !restoredState.isLoggedIn) {
                                onCourseRegistrationLoginRequired?.invoke(courseRegistrationEntryMode)
                                    ?: onLoginRequired()
                            }
                            courseRegistrationIntentForEntry(
                                entryMode = courseRegistrationEntryMode,
                                dialog = restoredState.dialog,
                            )?.let(registrationViewModel::onIntent)
                            registrationEntryReady = true
                        }
                        if (registrationEntryReady) {
                            CourseRegistrationFlow(
                                onLoginRequired = {
                                    onCourseRegistrationLoginRequired?.invoke(courseRegistrationEntryMode)
                                        ?: onLoginRequired()
                                },
                                onExit = { backStack.popCourseRegistration() },
                                onComplete = { backStack.completeCourseRegistration() },
                                viewModel = registrationViewModel,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CourseRegistrationLoadingIndicator()
                            }
                        }
                    }
                    PracticeRecordsRoute -> NavEntry(key) {
                        PracticeRecordsScreen(
                            onBack = { backStack.removeAt(backStack.lastIndex) },
                            onWriteReviewClick = { placeId, placeName ->
                                backStack.add(ReviewWriteRoute(placeId, placeName))
                            },
                        )
                    }
                    MyPostsRoute -> NavEntry(key) {
                        MyPostsScreen(
                            onBack = { backStack.removeAt(backStack.lastIndex) },
                            onPracticeRecordsClick = { backStack.add(PracticeRecordsRoute) },
                            onEditReviewClick = { post ->
                                backStack.add(ReviewWriteRoute(post.placeId, post.placeName, post.review.reviewId))
                            },
                        )
                    }
                    is PracticeSkipReasonRoute -> NavEntry(key) {
                        PracticeSkipReasonScreen(
                            practiceId = key.practiceId,
                            onClose = { backStack.removeAt(backStack.lastIndex) },
                        )
                    }
                    is ReviewWriteRoute -> NavEntry(key) {
                        ReviewWriteScreen(
                            placeId = key.placeId,
                            placeName = key.placeName,
                            editingReviewId = key.reviewId,
                            onClose = { backStack.removeAt(backStack.lastIndex) },
                            onCompleted = {
                                homeViewModel.onIntent(HomeIntent.OnReviewUpdated)
                                backStack.removeAt(backStack.lastIndex)
                            },
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
        RodiSnackbarHost(courseRegistrationSnackbarHostState)
    }

    if (showResumeDialog) {
        CourseRegistrationResumeDialog(
            onStartFresh = {
                coroutineScope.launch {
                    courseRegistrationEntryViewModel.clearDraft()
                        .onSuccess { openCourseRegistration(CourseRegistrationEntryMode.StartFresh) }
                        .onFailure { error ->
                            courseRegistrationSnackbarHostState.show(
                                RodiSnackbarData(
                                    message = courseRegistrationClearDraftFailureMessage(error),
                                ),
                            )
                        }
                }
            },
            onContinue = {
                openCourseRegistration(CourseRegistrationEntryMode.ContinueDraft)
            },
        )
    }
}

internal fun MutableList<NavKey>.pushMyPage() {
    add(MyPageRoute)
}

internal fun MutableList<NavKey>.popMyPage() {
    if (lastOrNull() == MyPageRoute) removeAt(lastIndex)
}

internal fun MutableList<NavKey>.openCourseRegistration() {
    if (lastOrNull() != CourseRegistrationFlowRoute) add(CourseRegistrationFlowRoute)
}

internal fun MutableList<NavKey>.popCourseRegistration() {
    if (lastOrNull() == CourseRegistrationFlowRoute) removeAt(lastIndex)
}

internal fun MutableList<NavKey>.completeCourseRegistration() {
    clear()
    add(HomeRoute)
}

internal fun NavKey?.shouldShowMainBottomNavigation(): Boolean = this == HomeRoute || this == MyPageRoute

internal fun NavKey?.toBottomNavigationDestination(): RodiBottomNavigationDestination = when (this) {
    MyPageRoute -> RodiBottomNavigationDestination.My
    CourseRegistrationFlowRoute -> RodiBottomNavigationDestination.Register
    else -> RodiBottomNavigationDestination.Home
}

internal fun NavKey?.toMainBottomNavigationDestination(
    showResumeDialog: Boolean,
): RodiBottomNavigationDestination = if (showResumeDialog) {
    RodiBottomNavigationDestination.Register
} else {
    toBottomNavigationDestination()
}
