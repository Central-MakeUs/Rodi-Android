package com.dororong.rodi.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.valentinilk.shimmer.LocalShimmerTheme
import com.valentinilk.shimmer.shimmer

@Composable
fun RodiSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    color: Color = RodiTheme.colors.gray100,
    shimmerColors: List<Color>? = null,
) {
    if (shimmerColors == null) {
        Box(
            modifier = modifier
                .clip(shape)
                .shimmer()
                .background(color),
        )
    } else {
        val shimmerTheme = LocalShimmerTheme.current.copy(
            blendMode = BlendMode.SrcOver,
            shaderColors = shimmerColors,
            shaderColorStops = shimmerColors.indices.map { index ->
                index.toFloat() / (shimmerColors.lastIndex.coerceAtLeast(1))
            },
        )
        CompositionLocalProvider(LocalShimmerTheme provides shimmerTheme) {
            Box(
                modifier = modifier
                    .clip(shape)
                    .shimmer()
                    .background(color),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RodiSkeletonPreview() {
    RodiTheme {
        RodiSkeleton(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(72.dp),
        )
    }
}
