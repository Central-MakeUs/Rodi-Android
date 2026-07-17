package com.dororong.rodi.feature.settings.account

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.theme.RodiRadius
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.settings.SettingsTopBar

@Composable
fun AccountSettingsScreen(
    onBack: () -> Unit,
    onInquiryClick: () -> Unit,
    onSessionEnded: () -> Unit,
    viewModel: AccountSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingAction by remember { mutableStateOf<AccountAction?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AccountSettingsEffect.SessionEnded -> onSessionEnded()
                is AccountSettingsEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AccountSettingsContent(
        isSubmitting = uiState.isSubmitting,
        pendingAction = pendingAction,
        onBack = onBack,
        onInquiryClick = onInquiryClick,
        onLogoutClick = { pendingAction = AccountAction.Logout },
        onWithdrawClick = { pendingAction = AccountAction.Withdraw },
        onDismissDialog = { if (!uiState.isSubmitting) pendingAction = null },
        onConfirm = { action ->
            viewModel.confirm(action)
            pendingAction = null
        },
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun AccountSettingsContent(
    isSubmitting: Boolean,
    pendingAction: AccountAction?,
    onBack: () -> Unit,
    onInquiryClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    onDismissDialog: () -> Unit,
    onConfirm: (AccountAction) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RodiTheme.colors.white),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            SettingsTopBar(title = "개인정보 관리", onBack = onBack)
            AccountMenuItem(text = "문의하기", hasChevron = true, onClick = onInquiryClick)
            AccountMenuItem(text = "로그아웃", onClick = onLogoutClick)
            AccountMenuItem(text = "계정 삭제하기", onClick = onWithdrawClick)
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }

    pendingAction?.let { action ->
        AccountConfirmationDialog(
            action = action,
            isSubmitting = isSubmitting,
            onConfirm = { onConfirm(action) },
            onDismiss = onDismissDialog,
        )
    }
}

@Composable
private fun AccountMenuItem(
    text: String,
    hasChevron: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = RodiTheme.typography.body1Medium,
            color = RodiTheme.colors.black,
            modifier = Modifier.weight(1f),
        )
        if (hasChevron) {
            Icon(
                painter = painterResource(CoreUiR.drawable.ic_chevron_right),
                contentDescription = null,
                tint = RodiTheme.colors.gray600,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun AccountConfirmationDialog(
    action: AccountAction,
    isSubmitting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isWithdraw = action == AccountAction.Withdraw
    Dialog(onDismissRequest = { if (!isSubmitting) onDismiss() }) {
        Surface(
            modifier = Modifier.width(280.dp),
            shape = RoundedCornerShape(RodiRadius.md),
            color = RodiTheme.colors.white,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (isWithdraw) 24.dp else 16.dp),
            ) {
                Text(
                    text = if (isWithdraw) "정말 계정을 삭제하시겠습니까?" else "로그아웃 하시겠습니까?",
                    style = RodiTheme.typography.price1,
                    color = RodiTheme.colors.black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isWithdraw) {
                    Text(
                        text = "삭제 후 3일 이내 재로그인 시 복구 가능합니다. 10일 이후 재가입 가능합니다.",
                        style = RodiTheme.typography.caption1Medium,
                        color = RodiTheme.colors.black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DialogButton(
                        text = "예",
                        isPrimary = false,
                        enabled = !isSubmitting,
                        onClick = onConfirm,
                    )
                    DialogButton(
                        text = "아니오",
                        isPrimary = true,
                        enabled = !isSubmitting,
                        onClick = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogButton(
    text: String,
    isPrimary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(116.dp)
            .height(42.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(RodiRadius.sm),
        color = if (isPrimary) RodiTheme.colors.primary600 else RodiTheme.colors.white,
        border = if (isPrimary) null else BorderStroke(1.dp, RodiTheme.colors.gray300),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = RodiTheme.typography.button1,
                color = if (isPrimary) RodiTheme.colors.white else RodiTheme.colors.gray800,
            )
        }
    }
}

@Composable
fun InquiryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    InquiryContent(
        onBack = onBack,
        onEmailClick = {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:yangyunseo71@gmail.com")),
                )
            } catch (_: ActivityNotFoundException) {
            }
        },
    )
}

@Composable
private fun InquiryContent(
    onBack: () -> Unit,
    onEmailClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = RodiTheme.colors.white,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            SettingsTopBar(title = "문의하기", onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEmailClick)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
            ) {
                Text(
                    text = "문의 이메일",
                    style = RodiTheme.typography.body1Medium,
                    color = RodiTheme.colors.black,
                )
                Text(
                    text = "yangyunseo71@gmail.com로 연락바랍니다.",
                    style = RodiTheme.typography.body3Medium,
                    color = RodiTheme.colors.gray800,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 375, heightDp = 812)
@Composable
private fun AccountSettingsContentPreview() {
    RodiTheme {
        AccountSettingsContent(
            isSubmitting = false,
            pendingAction = null,
            onBack = {},
            onInquiryClick = {},
            onLogoutClick = {},
            onWithdrawClick = {},
            onDismissDialog = {},
            onConfirm = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 375, heightDp = 812)
@Composable
private fun AccountWithdrawDialogPreview() {
    RodiTheme {
        AccountSettingsContent(
            isSubmitting = false,
            pendingAction = AccountAction.Withdraw,
            onBack = {},
            onInquiryClick = {},
            onLogoutClick = {},
            onWithdrawClick = {},
            onDismissDialog = {},
            onConfirm = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
