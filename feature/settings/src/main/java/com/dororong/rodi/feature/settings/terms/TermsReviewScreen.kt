package com.dororong.rodi.feature.settings.terms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.terms.TermsDocument
import com.dororong.rodi.core.ui.terms.TermsDocuments
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.settings.SettingsTopBar

@Composable
fun TermsReviewScreen(
    onBack: () -> Unit,
    onTermsClick: (TermsDocument) -> Unit,
) {
    TermsReviewContent(
        onBack = onBack,
        onTermsClick = onTermsClick,
    )
}

@Composable
private fun TermsReviewContent(
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
            SettingsTopBar(title = "약관 다시보기", onBack = onBack)
            TermsDocuments.ALL.forEach { document ->
                TermsMenuItem(document = document, onClick = { onTermsClick(document) })
            }
        }
    }
}

@Composable
private fun TermsMenuItem(
    document: TermsDocument,
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
            text = document.title,
            style = RodiTheme.typography.body1Medium,
            color = RodiTheme.colors.black,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(CoreUiR.drawable.ic_chevron_right),
            contentDescription = null,
            tint = RodiTheme.colors.gray600,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 375, heightDp = 812)
@Composable
private fun TermsReviewContentPreview() {
    RodiTheme {
        TermsReviewContent(onBack = {}, onTermsClick = {})
    }
}
