package com.dororong.rodi.feature.auth

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarData
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHost
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHostState
import com.dororong.rodi.core.ui.effect.CollectEffect
import com.dororong.rodi.core.ui.theme.RodiSpacing
import com.dororong.rodi.core.ui.theme.RodiTheme
import dagger.hilt.android.EntryPointAccessors

@Composable
fun LoginScreen(
    onNavigateNext: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val activity = LocalActivity.current
    val kakaoLoginManager = remember(activity) {
        activity?.let {
            EntryPointAccessors.fromActivity(
                it,
                KakaoLoginManagerEntryPoint::class.java,
            ).kakaoLoginManager()
        }
    }
    val snackbarHostState = remember { RodiSnackbarHostState() }
    val uiState by viewModel.uiState.collectAsState()

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            LoginEffect.NavigateNext -> onNavigateNext()
            is LoginEffect.ShowSnackbar ->
                snackbarHostState.show(RodiSnackbarData(message = effect.message))
        }
    }

    LoginContent(
        uiState = uiState,
        onKakaoLoginClick = {
            val manager = kakaoLoginManager
            if (manager != null) {
                manager.login(
                    onSuccess = viewModel::onKakaoLoginResult,
                    onFailure = viewModel::onKakaoLoginFailed,
                )
            } else {
                viewModel.onKakaoLoginFailed("로그인을 진행할 수 없습니다. 다시 시도해주세요.")
            }
        },
        onSkipClick = viewModel::onSkipClick,
    )
    RodiSnackbarHost(snackbarHostState)
}

@Composable
fun LoginContent(
    uiState: LoginUiState,
    onKakaoLoginClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        TextButton(
            onClick = onSkipClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(RodiSpacing.md),
        ) {
            Text(
                text = "둘러보기",
                style = RodiTheme.typography.body2Medium,
                color = RodiTheme.colors.gray500,
            )
        }
        Text(
            text = "Rodi",
            modifier = Modifier.align(Alignment.Center),
            style = RodiTheme.typography.heading1,
            color = RodiTheme.colors.primary600,
        )
        KakaoLoginButton(
            onClick = onKakaoLoginClick,
            enabled = uiState != LoginUiState.LoggingIn,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = RodiSpacing.md, vertical = 40.dp)
                .fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun LoginContentPreview() {
    RodiTheme {
        LoginContent(
            uiState = LoginUiState.Idle,
            onKakaoLoginClick = {},
            onSkipClick = {},
        )
    }
}
