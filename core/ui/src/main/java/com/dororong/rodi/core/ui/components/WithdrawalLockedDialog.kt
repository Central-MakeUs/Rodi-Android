package com.dororong.rodi.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.dororong.rodi.core.ui.components.dialog.RodiAlertDialog
import com.dororong.rodi.core.ui.theme.RodiTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 복구 기간이 지나 재가입을 기다리는 계정 안내. 날짜는 서버가 준 재가입 가능 시각을 그대로 쓴다. */
@Composable
fun WithdrawalLockedDialog(
    reRegisterableAt: Instant,
    onDismiss: () -> Unit,
) {
    RodiAlertDialog(
        title = "탈퇴 처리 중 계정",
        description = "${reRegisterableAt.toMonthDayText()} 이후 재가입 가능해요.",
        confirmText = "확인",
        onConfirm = onDismiss,
        onDismissRequest = onDismiss,
    )
}

private val MonthDayFormatter = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN)

private fun Instant.toMonthDayText(): String = atZone(ZoneId.systemDefault()).format(MonthDayFormatter)

@Preview(name = "재가입 대기 안내", showBackground = true, widthDp = 375, heightDp = 420)
@Composable
private fun WithdrawalLockedDialogPreview() {
    RodiTheme {
        WithdrawalLockedDialog(
            reRegisterableAt = Instant.parse("2026-09-20T03:00:00Z"),
            onDismiss = {},
        )
    }
}
