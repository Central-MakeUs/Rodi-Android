package com.dororong.rodi.core.ui.components.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiRadius
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.core.ui.R as CoreUiR

@Composable
fun KakaoLoginButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(RodiRadius.sm),
        colors = ButtonDefaults.buttonColors(
            containerColor = RodiTheme.semantic.brandKakao,
            contentColor = RodiTheme.semantic.onBrandKakao,
            disabledContainerColor = RodiTheme.semantic.brandKakao.copy(alpha = 0.4f),
            disabledContentColor = RodiTheme.semantic.onBrandKakao.copy(alpha = 0.4f),
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Icon(
                painter = painterResource(CoreUiR.drawable.ic_kakao),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(22.dp)
                    .alpha(if (enabled) 1f else 0.4f),
                tint = Color.Unspecified,
            )
            Text(
                text = "카카오로 시작하기",
                modifier = Modifier.align(Alignment.Center),
                style = RodiTheme.typography.body2SemiBold,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun KakaoLoginButtonPreview() {
    RodiTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KakaoLoginButton(onClick = {})
            KakaoLoginButton(onClick = {}, enabled = false)
        }
    }
}
