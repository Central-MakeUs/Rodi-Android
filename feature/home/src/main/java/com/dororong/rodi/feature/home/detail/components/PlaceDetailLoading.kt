package com.dororong.rodi.feature.home.detail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.components.RodiSkeleton
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.home.components.DismissibleSheetHandle

@Composable
fun PlaceDetailLoading(
    onHandleDragDown: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp),
    ) {
        DismissibleSheetHandle(
            onDragDown = onHandleDragDown,
            modifier = Modifier.height(24.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            RodiSkeleton(
                modifier = Modifier
                    .fillMaxWidth(0.28f)
                    .height(16.dp),
            )
            Spacer(Modifier.height(12.dp))
            RodiSkeleton(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(22.dp),
            )
            Spacer(Modifier.height(10.dp))
            RodiSkeleton(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(16.dp),
            )
            Spacer(Modifier.height(16.dp))
            RodiSkeleton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            )
        }
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
