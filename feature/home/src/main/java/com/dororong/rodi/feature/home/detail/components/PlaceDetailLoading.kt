package com.dororong.rodi.feature.home.detail.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun PlaceDetailLoading(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = RodiTheme.colors.primary600)
    }
}

@Preview(name = "Detail loading - compact", showBackground = true, widthDp = 375, heightDp = 240)
@Composable
private fun DetailLoadingCompactPreview() {
    RodiTheme { PlaceDetailLoading() }
}

@Preview(name = "Detail loading - small", showBackground = true, widthDp = 320, heightDp = 240, fontScale = 1.3f)
@Composable
private fun DetailLoadingSmallPreview() {
    RodiTheme { PlaceDetailLoading() }
}
