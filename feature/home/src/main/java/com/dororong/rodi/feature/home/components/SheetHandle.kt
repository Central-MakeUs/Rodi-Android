package com.dororong.rodi.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun SheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 60.dp, height = 4.dp)
                .background(RodiTheme.colors.handleBar, RoundedCornerShape(100.dp)),
        )
    }
}

@Preview(name = "Sheet handle", showBackground = true, widthDp = 360)
@Composable
private fun SheetHandlePreview() {
    RodiTheme { SheetHandle(Modifier.height(24.dp)) }
}
