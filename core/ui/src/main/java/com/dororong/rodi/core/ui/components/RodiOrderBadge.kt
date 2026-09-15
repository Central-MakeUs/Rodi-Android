package com.dororong.rodi.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun RodiOrderBadge(
    order: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(RodiTheme.colors.primary600),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = order.toString(),
            style = RodiTheme.typography.caption2Medium,
            color = RodiTheme.colors.white,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RodiOrderBadgePreview() {
    RodiTheme {
        RodiOrderBadge(order = 1)
    }
}
