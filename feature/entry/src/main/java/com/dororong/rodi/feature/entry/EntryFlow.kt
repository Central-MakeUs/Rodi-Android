package com.dororong.rodi.feature.entry

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarData
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHost
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHostState
import com.dororong.rodi.core.ui.effect.CollectEffect
import com.dororong.rodi.core.ui.terms.TermsWebView
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.entry.component.OnboardingAnalysisDialog
import com.dororong.rodi.feature.entry.component.LocalStepProgressAnimationState
import com.dororong.rodi.feature.entry.component.rememberStepProgressAnimationState
import com.dororong.rodi.feature.entry.content.CareerContent
import com.dororong.rodi.feature.entry.content.DrivingPrecautionsContent
import com.dororong.rodi.feature.entry.content.LocationPermissionContent
import com.dororong.rodi.feature.entry.content.NicknameContent
import com.dororong.rodi.feature.entry.content.PreferenceContent
import com.dororong.rodi.feature.entry.content.TermsAgreementContent
import com.dororong.rodi.core.ui.R as CoreUiR

/**
 * 진입 게이트 호스트: 약관 → 닉네임 → 경력 → 선호 → 주의사항 → 위치권한 순으로 상태 머신을 전환한다.
 * 마지막 단계 완료 시 [onComplete].
 */
@Composable
fun EntryFlow(
    onComplete: () -> Unit,
    viewModel: EntryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (!state.isRestored) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RodiTheme.colors.white),
        )
        return
    }

    val snackbarHostState = remember { RodiSnackbarHostState() }
    val networkErrorIcon = painterResource(CoreUiR.drawable.ic_alert_circle)
    val stepProgressAnimationState = rememberStepProgressAnimationState()

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            EntryEffect.CompleteEntry -> onComplete()
            is EntryEffect.ShowSubmissionError -> snackbarHostState.show(
                RodiSnackbarData(
                    message = effect.message,
                    icon = networkErrorIcon,
                    actionLabel = "새로고침".takeIf { effect.canRetry },
                    onAction = viewModel::startOnboardingAnalysis.takeIf { effect.canRetry },
                ),
            )
        }
    }

    BackHandler(enabled = state.step != EntryStep.TERMS) { viewModel.back() }

    CompositionLocalProvider(LocalStepProgressAnimationState provides stepProgressAnimationState) {
        Box(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = state.step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "entryStep",
            ) { target ->
                when (target) {
                    EntryStep.LOCATION -> LocationPermissionContent(
                        onBack = { viewModel.back() },
                        onPermissionResolved = viewModel::finish,
                    )

                    EntryStep.TERMS -> TermsAgreementContent(
                        service = state.serviceTermsChecked,
                        privacy = state.privacyTermsChecked,
                        location = state.locationTermsChecked,
                        onAllToggle = viewModel::setAllTermsChecked,
                        onServiceToggle = viewModel::toggleServiceTerms,
                        onPrivacyToggle = viewModel::togglePrivacyTerms,
                        onLocationToggle = viewModel::toggleLocationTerms,
                        onBack = null,
                        onNext = viewModel::next,
                        onTermsClick = { url -> viewModel.openWebView(url) },
                    )

                    EntryStep.PRECAUTIONS -> DrivingPrecautionsContent(
                        license = state.licenseChecked,
                        companion = state.companionChecked,
                        agree = state.precautionAgreementChecked,
                        onLicenseToggle = viewModel::toggleLicense,
                        onCompanionToggle = viewModel::toggleCompanion,
                        onAgreeToggle = viewModel::togglePrecautionAgreement,
                        onBack = { viewModel.back() },
                        onComplete = viewModel::next,
                    )

                    EntryStep.NICKNAME -> NicknameContent(
                        nickname = state.nickname,
                        onBack = { viewModel.back() },
                        onNext = viewModel::next,
                    )

                    EntryStep.CAREER -> CareerContent(
                        drivingPeriod = state.drivingPeriod,
                        recentFrequency = state.recentFrequency,
                        roadExperiences = state.roadExperiences,
                        soloDrivingRange = state.soloDrivingRange,
                        soloParkingLevel = state.soloParkingLevel,
                        nextEnabled = state.isCareerStepValid,
                        onDrivingPeriodSelect = viewModel::selectDrivingPeriod,
                        onRecentFrequencySelect = viewModel::selectRecentFrequency,
                        onRoadExperienceToggle = viewModel::toggleRoadExperience,
                        onSoloDrivingRangeSelect = viewModel::selectSoloDrivingRange,
                        onSoloParkingLevelSelect = viewModel::selectSoloParkingLevel,
                        onBack = { viewModel.back() },
                        onNext = viewModel::continueAfterCareer,
                    )

                    EntryStep.PREFERENCE -> PreferenceContent(
                        practiceSituations = state.practiceSituations,
                        vehicleType = state.vehicleType,
                        goal = state.goal,
                        nextEnabled = state.isPreferenceNextEnabled,
                        onPracticeSituationToggle = viewModel::togglePracticeSituation,
                        onVehicleTypeSelect = viewModel::selectVehicleType,
                        onGoalChange = viewModel::updateGoal,
                        onBack = { viewModel.back() },
                        onSkip = viewModel::startOnboardingAnalysis,
                        onNext = viewModel::startOnboardingAnalysis,
                    )

                    EntryStep.TERMS_WEBVIEW -> TermsWebView(
                        url = state.webViewUrl,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            state.onboardingAnalysisState?.let { analysisState ->
                state.onboardingLevel?.let { level ->
                    OnboardingAnalysisDialog(
                        state = analysisState,
                        level = level,
                        onConfirm = viewModel::continueAfterOnboardingAnalysis,
                    )
                }
            }
            RodiSnackbarHost(snackbarHostState)
        }
    }
}
