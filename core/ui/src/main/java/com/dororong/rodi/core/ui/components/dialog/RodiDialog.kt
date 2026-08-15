package com.dororong.rodi.core.ui.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.dororong.rodi.core.ui.R
import com.dororong.rodi.core.ui.components.button.RodiButton
import com.dororong.rodi.core.ui.components.button.RodiButtonVariant
import com.dororong.rodi.core.ui.components.button.RodiIconButton
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun RodiDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(24.dp),
    dismissible: Boolean = true,
    showCloseButton: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalInspectionMode.current) {
        RodiDialogSurface(
            modifier = modifier,
            contentPadding = contentPadding,
            showCloseButton = showCloseButton,
            onDismissRequest = onDismissRequest,
            content = content,
        )
    } else {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                dismissOnBackPress = dismissible,
                dismissOnClickOutside = dismissible,
            ),
        ) {
            val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
            SideEffect { dialogWindowProvider?.window?.setDimAmount(0.5f) }
            RodiDialogSurface(
                modifier = modifier,
                contentPadding = contentPadding,
                showCloseButton = showCloseButton,
                onDismissRequest = onDismissRequest,
                content = content,
            )
        }
    }
}

@Composable
private fun RodiDialogSurface(
    modifier: Modifier,
    contentPadding: PaddingValues,
    showCloseButton: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = RodiTheme.colors.white,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (showCloseButton) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    RodiIconButton(
                        painter = painterResource(R.drawable.ic_x),
                        onClick = onDismissRequest,
                        contentDescription = "닫기",
                        tint = RodiTheme.colors.gray600,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }
            }
            content()
        }
    }
}

@Composable
fun RodiAlertDialog(
    confirmText: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    descriptionMaxLines: Int = Int.MAX_VALUE,
    dismissText: String? = null,
    onDismiss: (() -> Unit)? = null,
    enabled: Boolean = true,
    dismissible: Boolean = true,
    showCloseButton: Boolean = false,
    titleContent: (@Composable () -> Unit)? = null,
) {
    val usesCompactActionSpacing = LocalDensity.current.fontScale > 1f
    val horizontalContentPadding = if (usesCompactActionSpacing) 12.dp else 24.dp

    RodiDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.width(280.dp),
        contentPadding = PaddingValues(
            horizontal = horizontalContentPadding,
            vertical = 24.dp,
        ),
        dismissible = dismissible,
        showCloseButton = showCloseButton,
    ) {
        titleContent?.invoke() ?: title?.let {
            Text(
                text = it,
                modifier = Modifier.fillMaxWidth(),
                style = RodiTheme.typography.headline1,
                color = RodiTheme.colors.black,
                textAlign = TextAlign.Center,
            )
        }
        description?.let {
            Text(
                text = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.gray600,
                textAlign = TextAlign.Center,
                maxLines = descriptionMaxLines,
                overflow = TextOverflow.Clip,
            )
        }
        if (dismissText == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                RodiButton(
                    text = confirmText,
                    onClick = onConfirm,
                    modifier = Modifier.width(116.dp),
                    enabled = enabled,
                    fillMaxWidth = false,
                )
            }
        } else {
            if (usesCompactActionSpacing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RodiButton(
                        text = dismissText,
                        onClick = onDismiss ?: onDismissRequest,
                        modifier = Modifier.fillMaxWidth(),
                        variant = RodiButtonVariant.Secondary,
                        enabled = enabled,
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
                    )
                    RodiButton(
                        text = confirmText,
                        onClick = onConfirm,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RodiButton(
                        text = dismissText,
                        onClick = onDismiss ?: onDismissRequest,
                        modifier = Modifier.weight(1f),
                        variant = RodiButtonVariant.Secondary,
                        enabled = enabled,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    )
                    RodiButton(
                        text = confirmText,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        enabled = enabled,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun RodiUnsavedChangesDialog(
    onContinueWriting: () -> Unit,
    onExit: () -> Unit,
    dismissible: Boolean = true,
) {
    RodiDialog(
        onDismissRequest = onContinueWriting,
        modifier = Modifier
            .width(280.dp)
            .height(226.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 32.dp),
        dismissible = dismissible,
    ) {
        Text(
            text = "작성 중인 화면을 나갈까요?",
            style = RodiTheme.typography.price1,
            color = RodiTheme.colors.black,
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "나가면 입력한 내용이 저장되지 않아요.",
                style = RodiTheme.typography.caption1Medium,
                color = RodiTheme.colors.black,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RodiButton(
                text = "나가기",
                onClick = onExit,
                modifier = Modifier.width(116.dp),
                variant = RodiButtonVariant.Secondary,
                fillMaxWidth = false,
                height = 42.dp,
            )
            RodiButton(
                text = "계속 작성",
                onClick = onContinueWriting,
                modifier = Modifier.width(116.dp),
                fillMaxWidth = false,
                height = 42.dp,
            )
        }
    }
}

@Preview(name = "Dialog - confirm", showBackground = true)
@Composable
private fun RodiAlertDialogPreview() {
    RodiTheme {
        RodiAlertDialog(
            confirmText = "확인",
            onConfirm = {},
            onDismissRequest = {},
            title = "후기 등록을 완료했어요!",
            description = "다른 운전자에게 도움이 돼요.",
        )
    }
}

@Preview(name = "Dialog - choice", showBackground = true)
@Composable
private fun RodiAlertDialogChoicePreview() {
    RodiTheme {
        RodiAlertDialog(
            confirmText = "계속 작성",
            onConfirm = {},
            onDismissRequest = {},
            dismissText = "나가기",
            onDismiss = {},
            title = "작성 중인 화면을 나갈까요?",
            description = "나가면 입력한 내용이 저장되지 않아요.",
            showCloseButton = true,
        )
    }
}

@Preview(name = "Dialog - disabled", showBackground = true)
@Composable
private fun RodiAlertDialogDisabledPreview() {
    RodiTheme {
        RodiAlertDialog(
            confirmText = "확인",
            onConfirm = {},
            onDismissRequest = {},
            title = "잠시만 기다려주세요",
            enabled = false,
        )
    }
}

@Preview(name = "Dialog - custom title", showBackground = true)
@Composable
private fun RodiAlertDialogCustomTitlePreview() {
    RodiTheme {
        RodiAlertDialog(
            confirmText = "다녀왔어요",
            onConfirm = {},
            onDismissRequest = {},
            titleContent = {
                Text(
                    text = "'강남역 주변 코스'\n연습은 잘 다녀오셨나요?",
                    style = RodiTheme.typography.headline1,
                    color = RodiTheme.colors.black,
                )
            },
        )
    }
}

@Preview(name = "Dialog - close", showBackground = true)
@Composable
private fun RodiAlertDialogClosePreview() {
    RodiTheme {
        RodiAlertDialog(
            confirmText = "확인",
            onConfirm = {},
            onDismissRequest = {},
            title = "닫을 수 있는 팝업",
            showCloseButton = true,
        )
    }
}

@Preview(name = "Dialog - choice narrow large text", showBackground = true, widthDp = 280, fontScale = 1.5f)
@Composable
private fun RodiAlertDialogChoiceLargeTextPreview() {
    RodiTheme {
        RodiAlertDialog(
            confirmText = "다녀왔어요",
            onConfirm = {},
            onDismissRequest = {},
            dismissText = "안 했어요",
            onDismiss = {},
            title = "연습은 잘 다녀오셨나요?",
        )
    }
}

@Preview(name = "Dialog - 작성 이탈", showBackground = true, widthDp = 360)
@Composable
private fun RodiUnsavedChangesDialogPreview() {
    RodiTheme {
        RodiUnsavedChangesDialog(onContinueWriting = {}, onExit = {})
    }
}
