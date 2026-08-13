package com.dororong.rodi.feature.mypage.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.components.button.RodiIconButton
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
internal fun MyPageTopBar(onSettingsClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "마이페이지",
            style = RodiTheme.typography.headline1,
            color = RodiTheme.colors.black,
        )
        RodiIconButton(
            painter = painterResource(CoreUiR.drawable.ic_settings),
            onClick = onSettingsClick,
            contentDescription = "설정",
            tint = RodiTheme.colors.black,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun MyPageTopBarPreview() {
    RodiTheme {
        MyPageTopBar(onSettingsClick = {})
    }
}
