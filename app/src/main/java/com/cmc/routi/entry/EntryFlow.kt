package com.cmc.routi.entry

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * 진입 게이트 호스트: 위치권한 → 약관 → 주의사항 3단계를 상태 머신으로 전환한다.
 * 마지막 단계 완료 시 [onComplete].
 */
@Composable
fun EntryFlow(
    onComplete: () -> Unit,
    viewModel: EntryViewModel = viewModel(),
) {
    val step = viewModel.step

    BackHandler(enabled = step != EntryStep.LOCATION) { viewModel.back() }

    AnimatedContent(
        targetState = step,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "entryStep",
    ) { target ->
        when (target) {
            EntryStep.LOCATION -> LocationPermissionContent(
                onPermissionResolved = viewModel::next,
            )

            EntryStep.TERMS -> TermsAgreementContent(
                onBack = { viewModel.back() },
                onNext = viewModel::next,
                onTermsClick = { url -> viewModel.openWebView(url) },
            )

            EntryStep.PRECAUTIONS -> DrivingPrecautionsContent(
                onBack = { viewModel.back() },
                onComplete = { viewModel.complete(onComplete) },
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
