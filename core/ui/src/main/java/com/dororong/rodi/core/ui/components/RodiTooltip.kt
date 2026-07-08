package com.dororong.rodi.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dororong.rodi.core.ui.theme.RodiTheme

@Composable
fun RodiTooltip(
    text: String,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = RodiTheme.colors.primary600

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(6.dp),
                    ambientColor = RodiTheme.colors.black.copy(alpha = 0.18f),
                    spotColor = RodiTheme.colors.black.copy(alpha = 0.18f),
                )
                .background(backgroundColor, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 9.dp),
        ) {
            Text(
                text = text,
                style = RodiTheme.typography.body3SemiBold.copy(lineHeight = 16.sp),
                color = RodiTheme.colors.white,
            )
        }
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 8.dp)
                .drawBehind { drawBottomTail(backgroundColor) },
        )
    }
}

private fun DrawScope.drawBottomTail(color: androidx.compose.ui.graphics.Color) {
    val path = Path().apply {
        moveTo(0f, -0.5f)
        lineTo(size.width, -0.5f)
        lineTo(size.width / 2f, size.height)
        close()
    }
    drawPath(path, color)
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RodiTooltipPreview() {
    RodiTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            RodiTooltip(text = "최근에 로그인했어요!")
        }
    }
}
