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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.core.ui.terms.TermsWebView

/**
 * 진입 게이트 호스트: 약관 → 닉네임 → 경력 → 선호 → 주의사항 → 위치권한 순으로 상태 머신을 전환한다.
 * 마지막 단계 완료 시 [onComplete].
 */
@Composable
fun EntryFlow(
    onComplete: () -> Unit,
    viewModel: EntryViewModel = hiltViewModel(),
) {
    if (!viewModel.isRestored) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RodiTheme.colors.white),
        )
        return
    }

    val step = viewModel.step

    BackHandler(enabled = step != EntryStep.TERMS) { viewModel.back() }

    AnimatedContent(
        targetState = step,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "entryStep",
    ) { target ->
        when (target) {
            EntryStep.LOCATION -> LocationPermissionContent(
                onBack = { viewModel.back() },
                onPermissionResolved = viewModel::submitOnboarding,
            )

            EntryStep.ANALYZING -> OnboardingAnalysisContent(
                isFailed = viewModel.submissionFailed,
                onRetry = viewModel::submitOnboarding,
            )

            EntryStep.RESULT -> OnboardingResultContent(
                level = requireNotNull(viewModel.onboardingLevel),
                onStart = { viewModel.finish(onComplete) },
            )

            EntryStep.TERMS -> TermsAgreementContent(
                service = viewModel.serviceTermsChecked,
                privacy = viewModel.privacyTermsChecked,
                location = viewModel.locationTermsChecked,
                onAllToggle = viewModel::setAllTermsChecked,
                onServiceToggle = viewModel::toggleServiceTerms,
                onPrivacyToggle = viewModel::togglePrivacyTerms,
                onLocationToggle = viewModel::toggleLocationTerms,
                onBack = null,
                onNext = viewModel::next,
                onTermsClick = { url -> viewModel.openWebView(url) },
            )

            EntryStep.PRECAUTIONS -> DrivingPrecautionsContent(
                license = viewModel.licenseChecked,
                companion = viewModel.companionChecked,
                agree = viewModel.precautionAgreementChecked,
                onLicenseToggle = viewModel::toggleLicense,
                onCompanionToggle = viewModel::toggleCompanion,
                onAgreeToggle = viewModel::togglePrecautionAgreement,
                onBack = { viewModel.back() },
                onComplete = viewModel::next,
            )

            EntryStep.NICKNAME -> {
                LaunchedEffect(Unit) { viewModel.ensureNicknameGenerated() }
                NicknameContent(
                    nickname = viewModel.nickname,
                    onBack = { viewModel.back() },
                    onNext = viewModel::next,
                )
            }

            EntryStep.CAREER -> CareerContent(
                drivingPeriod = viewModel.drivingPeriod,
                recentFrequency = viewModel.recentFrequency,
                roadExperiences = viewModel.roadExperiences,
                soloDrivingRange = viewModel.soloDrivingRange,
                soloParkingLevel = viewModel.soloParkingLevel,
                nextEnabled = viewModel.isCareerStepValid,
                onDrivingPeriodSelect = viewModel::selectDrivingPeriod,
                onRecentFrequencySelect = viewModel::selectRecentFrequency,
                onRoadExperienceToggle = viewModel::toggleRoadExperience,
                onSoloDrivingRangeSelect = viewModel::selectSoloDrivingRange,
                onSoloParkingLevelSelect = viewModel::selectSoloParkingLevel,
                onBack = { viewModel.back() },
                onNext = viewModel::next,
            )

            EntryStep.PREFERENCE -> PreferenceContent(
                practiceSituations = viewModel.practiceSituations,
                vehicleType = viewModel.vehicleType,
                goal = viewModel.goal,
                nextEnabled = viewModel.isPreferenceNextEnabled,
                onPracticeSituationToggle = viewModel::togglePracticeSituation,
                onVehicleTypeSelect = viewModel::selectVehicleType,
                onGoalChange = viewModel::updateGoal,
                onBack = { viewModel.back() },
                onSkip = viewModel::next,
                onNext = viewModel::next,
            )

            EntryStep.TERMS_WEBVIEW -> {
                TermsWebView(
                    url = viewModel.webViewUrl,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
