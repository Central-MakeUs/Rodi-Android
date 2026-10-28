package com.dororong.rodi.feature.mypage.nickname

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.dororong.rodi.core.domain.model.member.NicknameRejection
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.components.button.RodiIconButton
import com.dororong.rodi.core.ui.components.error.RodiRetryError
import com.dororong.rodi.core.ui.components.input.rememberGraphemeTextFieldState
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarData
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHost
import com.dororong.rodi.core.ui.components.snackbar.RodiSnackbarHostState
import com.dororong.rodi.core.ui.effect.CollectEffect
import com.dororong.rodi.core.ui.text.graphemeLength
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.mypage.R
import kotlinx.coroutines.flow.first

@Composable
fun NicknameScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NicknameViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { RodiSnackbarHostState() }
    val errorIcon = painterResource(CoreUiR.drawable.ic_alert_circle)
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnBack by rememberUpdatedState(onBack)

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.uiState.first { it.saveSucceeded }
            currentOnBack()
        }
    }
    CollectEffect(viewModel.effect) { effect ->
        snackbar.show(
            RodiSnackbarData(
                message = when (effect) {
                    NicknameEffect.CheckFailed -> "닉네임을 확인하지 못했어요. 잠시 후 다시 시도해주세요."
                    NicknameEffect.SaveFailed -> "닉네임을 저장하지 못했어요. 잠시 후 다시 시도해주세요."
                },
                icon = errorIcon,
            ),
        )
    }
    BackHandler { if (!uiState.isSaving) onBack() }
    Box(modifier.fillMaxSize()) {
        NicknameContent(
            uiState = uiState,
            onBack = { if (!uiState.isSaving) onBack() },
            onNicknameChange = viewModel::changeNickname,
            onValidate = viewModel::validate,
            onSave = viewModel::save,
            onRetry = viewModel::load,
        )
        RodiSnackbarHost(snackbar)
    }
}

@Composable
internal fun NicknameContent(
    uiState: NicknameUiState,
    onBack: () -> Unit,
    onNicknameChange: (String) -> Unit,
    onValidate: () -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val inspecting = LocalInspectionMode.current
    LaunchedEffect(uiState.isLoading, uiState.loadFailed) {
        if (!uiState.isLoading && !uiState.loadFailed && !inspecting) focusRequester.requestFocus()
    }
    Column(
        modifier.fillMaxSize().background(RodiTheme.colors.white).statusBarsPadding()
            .pointerInput(focusManager) { detectTapGestures { focusManager.clearFocus(); keyboard?.hide() } },
    ) {
        NicknameTopBar(canSave = uiState.canSave, onBack = onBack, onSave = onSave)
        when {
            uiState.loadFailed -> RodiRetryError("닉네임을 불러오지 못했어요.", onRetry)
            !uiState.isLoading -> Column(Modifier.padding(horizontal = 16.dp, vertical = 24.dp)) {
                Text(
                    "원하는 닉네임을 입력해주세요.",
                    style = RodiTheme.typography.body1SemiBold,
                    color = RodiTheme.colors.black,
                )
                Spacer(Modifier.height(12.dp))
                NicknameInput(
                    nickname = uiState.nickname,
                    onNicknameChange = onNicknameChange,
                    onValidate = onValidate,
                    focusRequester = focusRequester,
                    enabled = !uiState.isSaving,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = when {
                        uiState.canSave -> "사용 가능한 닉네임이에요."
                        uiState.isChecking -> "닉네임을 확인하고 있어요."
                        uiState.rejection != null -> uiState.rejection.guidance()
                        uiState.isChangeRestricted -> NICKNAME_CHANGE_GUIDANCE
                        uiState.validatedNickname != null -> "사용 가능한 닉네임이에요."
                        else -> NICKNAME_CHANGE_GUIDANCE
                    },
                    style = RodiTheme.typography.caption1SemiBold,
                    color = when {
                        uiState.canSave -> RodiTheme.colors.infoApproval
                        uiState.isChecking -> RodiTheme.colors.gray500
                        uiState.rejection != null && uiState.rejection != NicknameRejection.CHANGE_RESTRICTED -> RodiTheme.colors.infoCancel
                        uiState.isChangeRestricted -> RodiTheme.colors.gray500
                        uiState.validatedNickname != null -> RodiTheme.colors.infoApproval
                        else -> RodiTheme.colors.gray500
                    },
                )
            }
        }
    }
}

@Composable
private fun NicknameTopBar(canSave: Boolean, onBack: () -> Unit, onSave: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
        RodiIconButton(
            painter = painterResource(CoreUiR.drawable.ic_chevron_left),
            onClick = onBack,
            contentDescription = "뒤로가기",
            tint = RodiTheme.colors.black,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Text("닉네임 수정", style = RodiTheme.typography.headline1, color = RodiTheme.colors.black)
        RodiIconButton(
            painter = painterResource(R.drawable.ic_check),
            onClick = onSave,
            contentDescription = "완료",
            enabled = canSave,
            iconSize = 16.dp,
            tint = RodiTheme.colors.white,
            modifier = Modifier.align(Alignment.CenterEnd).background(
                if (canSave) RodiTheme.colors.primary600 else RodiTheme.colors.primary100,
                RoundedCornerShape(20.dp),
            ),
        )
    }
}

@Composable
private fun NicknameInput(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    onValidate: () -> Unit,
    focusRequester: FocusRequester,
    enabled: Boolean,
) {
    val text = rememberGraphemeTextFieldState(nickname, 10, onNicknameChange)
    var wasFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        BasicTextField(
            value = text.value,
            onValueChange = text.onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = RodiTheme.typography.body3Medium.copy(color = RodiTheme.colors.black),
            cursorBrush = SolidColor(RodiTheme.colors.black),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                keyboard?.hide()
            }),
            modifier = Modifier.fillMaxWidth().height(51.dp).focusRequester(focusRequester)
                .onFocusChanged {
                    if (wasFocused && !it.isFocused) onValidate()
                    wasFocused = it.isFocused
                }
                .border(1.dp, RodiTheme.colors.gray900, RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp),
            decorationBox = { inner -> Box(contentAlignment = Alignment.CenterStart) { inner() } },
        )
        Spacer(Modifier.height(4.dp))
        Text("${text.value.text.graphemeLength()} /10", style = RodiTheme.typography.body3Medium, color = RodiTheme.colors.gray600)
    }
}

private fun NicknameRejection.guidance(): String = when (this) {
    NicknameRejection.LENGTH -> "2자 이상 10자 이하만 사용 가능해요."
    NicknameRejection.CHARACTERS -> "한글, 영문, 숫자와 _만 사용할 수 있어요."
    NicknameRejection.PROHIBITED_WORD -> "사용할 수 없는 단어가 포함되어 있어요."
    NicknameRejection.DUPLICATE -> "이미 사용 중인 닉네임이에요."
    NicknameRejection.CHANGE_RESTRICTED -> NICKNAME_CHANGE_GUIDANCE
}

private const val NICKNAME_CHANGE_GUIDANCE = "닉네임은 변경 후 30일 동안 다시 바꿀 수 없어요."

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun NicknameDefaultPreview() {
    RodiTheme { NicknameContent(NicknameUiState(nickname = "운전하는_고양이", isLoading = false), {}, {}, {}, {}, {}) }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun NicknameAvailablePreview() {
    RodiTheme {
        NicknameContent(NicknameUiState(nickname = "운전하는_고양이", validatedNickname = "운전하는_고양이", normalizedNickname = "운전하는_고양이", isLoading = false), {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun NicknameRestrictedPreview() {
    RodiTheme {
        NicknameContent(
            NicknameUiState(nickname = "운전하는_고양이", isLoading = false, isChangeRestricted = true),
            {}, {}, {}, {}, {},
        )
    }
}
