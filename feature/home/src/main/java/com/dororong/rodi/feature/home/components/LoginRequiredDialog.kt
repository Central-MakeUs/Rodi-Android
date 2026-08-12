package com.dororong.rodi.feature.home.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dororong.rodi.core.ui.components.button.KakaoLoginButton
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.R

@Composable
fun LoginRequiredDialog(
    isLoggingIn: Boolean,
    onDismiss: () -> Unit,
    onKakaoLoginClick: () -> Unit,
) {
    // Dialog는 별도 윈도우라 정적 프리뷰에서 렌더되지 않는다. BackHandler도 프리뷰엔 디스패처가
    // 없어 터질 수 있으므로 함께 건너뛴다.
    if (LocalInspectionMode.current) {
        LoginRequiredDialogContent(isLoggingIn, onDismiss, onKakaoLoginClick)
        return
    }
    BackHandler(enabled = !isLoggingIn, onBack = onDismiss)
    Dialog(
        onDismissRequest = { if (!isLoggingIn) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !isLoggingIn, dismissOnClickOutside = !isLoggingIn),
    ) {
        LoginRequiredDialogContent(isLoggingIn, onDismiss, onKakaoLoginClick)
    }
}

@Composable
private fun LoginRequiredDialogContent(
    isLoggingIn: Boolean,
    onDismiss: () -> Unit,
    onKakaoLoginClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(290.dp)
            .height(348.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(RodiTheme.colors.white)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Icon(
                painter = painterResource(R.drawable.ic_x),
                contentDescription = "닫기",
                tint = RodiTheme.colors.black,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(enabled = !isLoggingIn, onClick = onDismiss),
            )
        }
        Spacer(Modifier.height(16.dp))
        Spacer(Modifier.weight(1f))
        Text(
            text = "로그인 후\n이용 가능한 기능이에요.",
            style = RodiTheme.typography.headline1,
            color = RodiTheme.colors.black,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Spacer(Modifier.weight(1f))
        KakaoLoginButton(
            onClick = onKakaoLoginClick,
            enabled = !isLoggingIn,
            height = 36.dp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(name = "Login required - default", showBackground = true, widthDp = 375, heightDp = 520)
@Composable
private fun LoginRequiredPreview() {
    RodiTheme { LoginRequiredDialog(false, {}, {}) }
}

@Preview(name = "Login required - progress", showBackground = true, widthDp = 375, heightDp = 520)
@Composable
private fun LoginProgressPreview() {
    RodiTheme { LoginRequiredDialog(true, {}, {}) }
}
