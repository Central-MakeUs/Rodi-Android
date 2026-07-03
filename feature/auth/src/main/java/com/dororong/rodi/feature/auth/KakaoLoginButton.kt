package com.dororong.rodi.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.components.RodiButtonDefaults
import com.dororong.rodi.core.ui.theme.RodiTheme

// 카카오 브랜드 가이드 고정 색상 — RodiTheme 토큰 대상 아님. SemanticColors 도입(BACKLOG) 시 이관 검토.
private val KakaoYellow = Color(0xFFFEE500)
private val KakaoContent = Color(0xFF191919)

@Composable
fun KakaoLoginButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(RodiButtonDefaults.Height),
        shape = RodiButtonDefaults.shape(),
        colors = ButtonDefaults.buttonColors(
            containerColor = KakaoYellow,
            contentColor = KakaoContent,
            disabledContainerColor = KakaoYellow,
            disabledContentColor = KakaoContent,
        ),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_kakao),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified,
            )
            Text(
                text = "카카오로 시작하기",
                style = RodiTheme.typography.button1,
            )
        }
    }
}
