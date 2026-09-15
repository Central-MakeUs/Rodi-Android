package com.dororong.rodi.core.ui.components.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiRadius
import com.dororong.rodi.core.ui.theme.RodiTheme

enum class RodiButtonVariant { Primary, Secondary }

/** RodiButton 기본값(높이/모양/색상)을 한 곳에서 계산하는 헬퍼. */
object RodiButtonDefaults {
    val Height = 48.dp

    @Composable
    fun shape(): Shape = RoundedCornerShape(RodiRadius.md)

    @Composable
    fun colors(variant: RodiButtonVariant): ButtonColors = when (variant) {
        RodiButtonVariant.Primary -> ButtonDefaults.buttonColors(
            containerColor = RodiTheme.colors.primary600,
            contentColor = RodiTheme.colors.white,
            disabledContainerColor = RodiTheme.colors.gray300,
            disabledContentColor = RodiTheme.colors.gray500,
        )

        RodiButtonVariant.Secondary -> ButtonDefaults.buttonColors(
            containerColor = RodiTheme.colors.white,
            contentColor = RodiTheme.colors.gray800,
            disabledContainerColor = RodiTheme.colors.gray100,
            disabledContentColor = RodiTheme.colors.gray400,
        )
    }

    @Composable
    fun border(variant: RodiButtonVariant): BorderStroke? = when (variant) {
        RodiButtonVariant.Primary -> null
        RodiButtonVariant.Secondary -> BorderStroke(1.dp, RodiTheme.colors.gray300)
    }
}

/**
 * Rodi 공용 버튼. 기본 높이 48dp, 기본 모양 [RodiRadius.md].
 * 기존 화면(ParkingDetailContent 등)처럼 모양을 다르게 써야 하면 [shape]를 오버라이드한다.
 */
@Composable
fun RodiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: RodiButtonVariant = RodiButtonVariant.Primary,
    enabled: Boolean = true,
    fillMaxWidth: Boolean = true,
    height: Dp = RodiButtonDefaults.Height,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    shape: Shape = RodiButtonDefaults.shape(),
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .let { if (fillMaxWidth) it.fillMaxWidth() else it }
            .height(height),
        shape = shape,
        colors = RodiButtonDefaults.colors(variant),
        border = RodiButtonDefaults.border(variant),
        contentPadding = contentPadding,
    ) {
        Text(
            text = text,
            modifier = Modifier.let { if (fillMaxWidth) it.fillMaxWidth() else it },
            style = RodiTheme.typography.button1,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}

@Preview(name = "RodiButton - Primary", showBackground = true, widthDp = 360)
@Composable
private fun RodiButtonPrimaryPreview() {
    RodiTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RodiButton(text = "확인", onClick = {})
            RodiButton(text = "비활성", onClick = {}, enabled = false)
        }
    }
}

@Preview(name = "RodiButton - Secondary", showBackground = true, widthDp = 360)
@Composable
private fun RodiButtonSecondaryPreview() {
    RodiTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RodiButton(text = "취소", onClick = {}, variant = RodiButtonVariant.Secondary)
            RodiButton(
                text = "비활성",
                onClick = {},
                variant = RodiButtonVariant.Secondary,
                enabled = false,
            )
        }
    }
}

@Preview(name = "RodiButton - narrow single line", showBackground = true, widthDp = 240)
@Composable
private fun RodiButtonNarrowPreview() {
    RodiTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            RodiButton(
                text = "다녀왔어요",
                onClick = {},
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            )
        }
    }
}
