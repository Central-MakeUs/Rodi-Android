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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarData
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHost
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHostState
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.course.registration.components.CourseRegistrationDialogHost
import com.dororong.rodi.feature.course.registration.components.CourseRegistrationSubmissionLoadingDialog
import com.dororong.rodi.feature.course.registration.content.CourseRegistrationFormContent
import com.dororong.rodi.feature.course.registration.content.CourseRegistrationMapContent
import com.dororong.rodi.feature.course.registration.content.CourseRegistrationTutorialContent

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
