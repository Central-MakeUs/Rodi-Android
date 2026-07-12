package com.dororong.rodi.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.terms.TermsDocument
import com.dororong.rodi.core.ui.terms.TermsDocuments
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.core.ui.R as CoreUiR

@Composable
fun SettingsContent(
    onBack: () -> Unit,
    onTermsClick: (TermsDocument) -> Unit,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(CoreUiR.drawable.ic_chevron_left),
                        contentDescription = "뒤로",
                        tint = RodiTheme.colors.black,
                    )
                }
                Text(
                    text = "설정",
                    style = RodiTheme.typography.headline1,
                    color = RodiTheme.colors.black,
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "약관 및 정책",
                style = RodiTheme.typography.body1SemiBold,
                color = RodiTheme.colors.black,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(8.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                TermsDocuments.ALL.forEach { document ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTermsClick(document) }
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = document.title,
                            style = RodiTheme.typography.body3Medium,
                            color = RodiTheme.colors.gray900,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            painter = painterResource(CoreUiR.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = RodiTheme.colors.gray600,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 1.dp,
                        color = RodiTheme.colors.gray100,
                    )
                }
            }
        }
    }
}

@Preview(name = "SettingsContent - Default", showBackground = true, widthDp = 360, heightDp = 812)
@Composable
private fun SettingsContentPreview() {
    RodiTheme {
        SettingsContent(
            onBack = {},
            onTermsClick = {},
        )
    }
}
