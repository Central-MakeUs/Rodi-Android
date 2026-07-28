package com.dororong.rodi.feature.auth

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dororong.rodi.core.ui.components.RodiTooltip
import com.dororong.rodi.core.ui.components.AccountRecoveryDialog
import com.dororong.rodi.core.ui.components.button.KakaoLoginButton
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarData
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHost
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHostState
import com.dororong.rodi.core.ui.effect.CollectEffect
import com.dororong.rodi.core.ui.theme.RodiSpacing
import com.dororong.rodi.core.ui.theme.RodiTheme
import dagger.hilt.android.EntryPointAccessors

@Composable
fun LoginScreen(
    onNavigateNext: (isNewMember: Boolean?) -> Unit,
    showRecentKakaoLogin: Boolean,
    sessionExpiredMessage: Boolean = false,
    onSessionExpiredMessageShown: () -> Unit = {},
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(sessionExpiredMessage) {
        if (sessionExpiredMessage) {
            snackbarHostState.show(RodiSnackbarData(message = "로그인 정보가 만료되어 다시 로그인해주세요."))
            onSessionExpiredMessageShown()
        }
    }

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is LoginEffect.NavigateNext -> onNavigateNext(effect.isNewMember)
            is LoginEffect.ShowSnackbar ->
                snackbarHostState.show(RodiSnackbarData(message = effect.message))
        }
    }

    LoginContent(
        uiState = uiState,
        showRecentKakaoLogin = showRecentKakaoLogin,
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
    (uiState as? LoginUiState.RecoveryRequired)?.let { recoveryState ->
        AccountRecoveryDialog(
            isRestoring = recoveryState.isRestoring,
            onConfirm = viewModel::onRecoveryConfirm,
            onDismiss = viewModel::onRecoveryDismiss,
        )
    }
}

@Composable
fun LoginContent(
    uiState: LoginUiState,
    showRecentKakaoLogin: Boolean,
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
        if (!showRecentKakaoLogin) {
            TextButton(
                onClick = onSkipClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = RodiSpacing.md),
            ) {
                Text(
                    text = "둘러보기",
                    style = RodiTheme.typography.caption2SemiBold,
                    color = RodiTheme.colors.gray500,
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-33.85f).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_rodi_logo),
                contentDescription = "Rodi",
            )
            Spacer(modifier = Modifier.height(RodiSpacing.sm))
            Text(
                text = "운전연습의 시작, 로디",
                style = RodiTheme.typography.body1Medium,
                color = RodiTheme.colors.black,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = RodiSpacing.md, vertical = 40.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (showRecentKakaoLogin) {
                RodiTooltip(text = "최근에 로그인했어요!")
                Spacer(modifier = Modifier.height(8.dp))
            }
            KakaoLoginButton(
                onClick = onKakaoLoginClick,
                enabled = uiState == LoginUiState.Idle,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(name = "Login - First", showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun LoginContentFirstPreview() {
    RodiTheme {
        LoginContent(
            uiState = LoginUiState.Idle,
            showRecentKakaoLogin = false,
            onKakaoLoginClick = {},
            onSkipClick = {},
        )
    }
}

@Preview(name = "Login - Recent Kakao", showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun LoginContentRecentKakaoPreview() {
    RodiTheme {
        LoginContent(
            uiState = LoginUiState.Idle,
            showRecentKakaoLogin = true,
            onKakaoLoginClick = {},
            onSkipClick = {},
        )
    }
}
