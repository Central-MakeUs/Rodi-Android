package com.dororong.rodi.feature.entry.content

import com.dororong.rodi.feature.entry.component.EntryScaffold
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun NicknameContent(
    nickname: String,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EntryScaffold(
        currentStep = 1,
        onBack = onBack,
        buttonText = "다음",
        buttonEnabled = true,
        onButtonClick = onNext,
        modifier = modifier,
        showProgress = true,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "닉네임",
                    style = RodiTheme.typography.headline1,
                    color = RodiTheme.colors.black,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "‘$nickname’",
                    style = RodiTheme.typography.heading1,
                    color = RodiTheme.colors.primary600,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "로 시작해요.",
                    style = RodiTheme.typography.headline1,
                    color = RodiTheme.colors.black,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun NicknameContentPreview() {
    RodiTheme {
        NicknameContent(
            nickname = "흐름타는 수달",
            onBack = {},
            onNext = {},
        )
    }
}
