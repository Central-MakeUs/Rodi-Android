package com.dororong.rodi.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiSpacing
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun RodiIllustratedEmptyState(
    painter: Painter,
    imageSize: Dp,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.size(imageSize),
            )
            Spacer(Modifier.height(RodiSpacing.md))
            Text(
                text = title,
                style = RodiTheme.typography.headline1,
                color = RodiTheme.colors.gray600,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(RodiSpacing.sm))
            Text(
                text = description,
                style = RodiTheme.typography.body3Medium,
                color = RodiTheme.colors.gray600,
                textAlign = TextAlign.Center,
            )
            footer()
        }
    }
}
