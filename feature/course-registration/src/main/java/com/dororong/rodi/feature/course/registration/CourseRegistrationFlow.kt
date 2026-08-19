package com.dororong.rodi.feature.course.registration

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.map.MapNetworkErrorScreen
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarData
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarDuration
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHost
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHostState
import com.dororong.rodi.core.ui.network.isNetworkAvailable
import com.dororong.rodi.core.ui.network.networkAvailabilityFlow
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.course.registration.components.CourseRegistrationDialogHost
import com.dororong.rodi.feature.course.registration.components.CourseRegistrationSubmissionLoadingDialog
import com.dororong.rodi.feature.course.registration.content.CourseRegistrationFormContent
import com.dororong.rodi.feature.course.registration.content.CourseRegistrationMapContent
import com.dororong.rodi.feature.course.registration.content.CourseRegistrationTutorialContent
import kotlinx.coroutines.delay

/** App owns navigation; this flow only reports login, exit, and completed events. */
@Composable
fun CourseRegistrationFlow(
    onLoginRequired: () -> Unit,
    onExit: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CourseRegistrationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { RodiSnackbarHostState() }
    val context = LocalContext.current
    var isOnline by remember { mutableStateOf(context.isNetworkAvailable()) }
    // 최초 진입이 오프라인이어도 3초 유예를 그대로 적용한다 — 아래 LaunchedEffect(isOnline)의
    // else 분기가 delay 후 이 값을 true로 바꾼다. 초기값을 true로 두면 그 유예를 건너뛴다.
    var showNetworkError by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        networkAvailabilityFlow(context).collect { isOnline = it }
    }
    // 오프라인이면 홈과 같은 안내 화면 + 재시도 스낵바를 띄운다(QA 4168:16565).
    // 토스트는 바로, 안내 화면은 유예 시간을 넘겨 계속 끊겨 있을 때만 덮는다. 다시 연결되면
    // 이 이펙트가 재시작되며 delay가 취소돼 원래 화면으로 돌아온다.
    val networkErrorIcon = painterResource(CoreUiR.drawable.ic_alert_circle)
    LaunchedEffect(isOnline) {
        if (isOnline) {
            snackbarHostState.dismiss(MAP_NETWORK_SNACKBAR_ID)
            if (showNetworkError) {
                showNetworkError = false
                viewModel.onIntent(CourseRegistrationIntent.Retry)
            }
        } else {
            snackbarHostState.showImmediately(
                RodiSnackbarData(
                    id = MAP_NETWORK_SNACKBAR_ID,
                    message = "네트워크 연결이 원활하지 않아요.\n다시 시도해볼까요?",
                    icon = networkErrorIcon,
                    duration = RodiSnackbarDuration.Indefinite,
                    actionLabel = "새로고침",
                    onAction = { viewModel.onIntent(CourseRegistrationIntent.Retry) },
                ),
            )
            delay(MAP_NETWORK_ERROR_GRACE_MILLIS)
            showNetworkError = true
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                CourseRegistrationEffect.LoginRequired -> onLoginRequired()
                CourseRegistrationEffect.Exit -> onExit()
                CourseRegistrationEffect.Completed -> onComplete()
                is CourseRegistrationEffect.ShowSnackbar -> snackbarHostState.show(
                    RodiSnackbarData(message = effect.message),
                )
            }
        }
    }
    BackHandler(enabled = state.isAuthResolved && state.isLoggedIn && state.dialog == null && !state.isSubmitting) {
        viewModel.onIntent(CourseRegistrationIntent.Back)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white),
    ) {
            when {
                !state.isAuthResolved -> CourseRegistrationLoadingScreen()
                !state.isLoggedIn -> CourseRegistrationLoginGate(onLoginRequired)
                state.page == CourseRegistrationPage.Tutorial -> CourseRegistrationTutorialContent(
                    page = state.tutorialPage,
                    isCompleting = state.tutorialLoadState == CourseTutorialLoadState.Completing,
                    onPageChanged = { viewModel.onIntent(CourseRegistrationIntent.TutorialPageChanged(it)) },
                    onBack = { viewModel.onIntent(CourseRegistrationIntent.Back) },
                    onComplete = { viewModel.onIntent(CourseRegistrationIntent.CompleteTutorial) },
                    isError = state.tutorialLoadState == CourseTutorialLoadState.Error,
                    onRetry = { viewModel.onIntent(CourseRegistrationIntent.Retry) },
                )
                state.page == CourseRegistrationPage.Map -> CourseRegistrationMapContent(
                    mapLoadState = state.mapLoadState,
                    mapRetryToken = state.mapRetryToken,
                    mapCenter = state.mapCenter,
                    mapCenterGeneration = state.mapCenterGeneration,
                    waypoints = state.waypoints,
                    route = state.route,
                    isRouteLoading = state.isRouteLoading,
                    isMapPointLoading = state.isMapPointLoading,
                    selectedWaypointRole = state.selectedWaypointRole,
                    editingWaypointIndex = state.editingWaypointIndex,
                    temporaryPin = state.temporaryPin,
                    isSearchVisible = state.isSearchVisible,
                    searchKeyword = state.searchKeyword,
                    searchLoading = state.isSearchLoading,
                    recentSearchLoading = state.isRecentSearchLoading,
                    searchResult = state.searchResult,
                    searchError = state.searchError,
                    canFinish = state.canFinishMap,
                    onIntent = viewModel::onIntent,
                    onBack = { viewModel.onIntent(CourseRegistrationIntent.Back) },
                    maxVias = state.registrationForm?.maxWaypoints ?: 4,
                    isFormLoading = state.formLoadState == CourseRegistrationFormLoadState.Loading,
                    pendingSuggestion = state.pendingSuggestion,
                    isPendingAddressLoading = state.isPendingAddressLoading,
                    initialLocationState = state.initialLocationState,
                )
                else -> CourseRegistrationFormContent(
                    loadState = state.formLoadState,
                    form = state.registrationForm,
                    waypoints = state.waypoints,
                    route = state.route,
                    selectedCategoryCodes = state.selectedCategoryCodes,
                    selectedPracticeTypeCodes = state.selectedPracticeTypeCodes,
                    caution = state.caution,
                    description = state.description,
                    isSubmitting = state.isSubmitting,
                    showValidationErrors = state.hasAttemptedSubmit,
                    canSubmit = state.canSubmit,
                    error = state.submissionError,
                    onIntent = viewModel::onIntent,
                    onBack = {
                        if (!state.isSubmitting) viewModel.onIntent(CourseRegistrationIntent.Back)
                    },
                )
            }
            if (showNetworkError && state.page == CourseRegistrationPage.Map) {
                MapNetworkErrorScreen()
            }
            if (state.isSubmitting) {
                CourseRegistrationSubmissionLoadingDialog()
            } else state.dialog?.let { dialog ->
                CourseRegistrationDialogHost(
                    dialog = dialog,
                    onContinueDraft = { viewModel.onIntent(CourseRegistrationIntent.ContinueDraft) },
                    onDiscardDraft = { viewModel.onIntent(CourseRegistrationIntent.DiscardDraft) },
                    onContinueWriting = { viewModel.onIntent(CourseRegistrationIntent.DismissDialog) },
                    onExit = { viewModel.onIntent(CourseRegistrationIntent.ConfirmExit) },
                    onSuccessConfirmed = { viewModel.onIntent(CourseRegistrationIntent.SuccessConfirmed) },
                )
            }
            RodiSnackbarHost(snackbarHostState)
        }
}

@Composable
private fun CourseRegistrationLoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CourseRegistrationLoadingIndicator()
    }
}

@Composable
private fun CourseRegistrationLoginGate(onLoginRequired: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(RodiTheme.colors.white).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("로그인 후 코스를 등록할 수 있어요", style = RodiTheme.typography.headline1, color = RodiTheme.colors.black, textAlign = TextAlign.Center)
        Text("나만의 연습 코스를 만들어 보세요.", style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray600, modifier = Modifier.padding(top = 12.dp))
        RodiButton(text = "로그인하기", onClick = onLoginRequired, modifier = Modifier.padding(top = 24.dp))
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun CourseRegistrationLoadingScreenPreview() {
    RodiTheme { CourseRegistrationLoadingScreen() }
}

private const val MAP_NETWORK_SNACKBAR_ID = "course-registration-map-network"

/** 오프라인이 이만큼 이어지면 지도를 덮고 안내 화면을 띄운다. 홈과 같은 값. */
private const val MAP_NETWORK_ERROR_GRACE_MILLIS = 3_000L
