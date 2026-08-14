package com.dororong.rodi.core.ui.components.button

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.R
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun RodiIconButton(
    painter: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    touchSize: Dp = 48.dp,
    layoutSize: Dp = 24.dp,
    rippleRadius: Dp = maxOf(touchSize / 2, 24.dp),
    contentDescription: String? = null,
    tint: Color = Color.Unspecified,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .layout { measurable, constraints ->
                val touchSizePx = touchSize.roundToPx()
                val layoutSizePx = layoutSize.roundToPx()
                val placeable = measurable.measure(
                    constraints.copy(
                        minWidth = touchSizePx,
                        minHeight = touchSizePx,
                        maxWidth = touchSizePx,
                        maxHeight = touchSizePx,
                    ),
                )

                layout(layoutSizePx, layoutSizePx) {
                    val offset = (layoutSizePx - touchSizePx) / 2
                    placeable.placeRelative(offset, offset)
                }
            }
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = false,
                    radius = rippleRadius,
                ),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RodiIconButtonPreview() {
    RodiTheme {
        Row(Modifier.padding(24.dp)) {
            RodiIconButton(
                painter = painterResource(R.drawable.ic_settings),
                onClick = {},
                iconSize = 20.dp,
                touchSize = 48.dp,
                layoutSize = 23.dp,
                contentDescription = "설정",
                tint = RodiTheme.colors.black,
            )
        }
    }
}
