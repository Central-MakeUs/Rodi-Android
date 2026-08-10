package com.dororong.rodi.feature.home.review.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.theme.RodiTheme

private val ScalePitch = 71.dp
private val ScaleCircleSize = 16.dp
private val ScaleTouchSize = 48.dp
private val ScaleLabelTop = 24.dp

@Composable
fun <T> ScalePicker(
    values: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pitch = ScalePitch
    val circleSize = ScaleCircleSize
    val circleOffset = (ScaleTouchSize - ScaleCircleSize) / 2
    val connectorColor = RodiTheme.colors.primary300
    Layout(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup()
            .drawBehind {
                val pitchPx = pitch.toPx()
                val circleSizePx = circleSize.toPx()
                val centerY = circleSizePx / 2
                repeat((values.size - 1).coerceAtLeast(0)) { index ->
                    drawLine(
                        color = connectorColor,
                        start = Offset(index * pitchPx + circleOffset.toPx() + circleSizePx, centerY),
                        end = Offset((index + 1) * pitchPx + circleOffset.toPx(), centerY),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            },
        content = {
            values.forEach { value ->
                ScaleTouchTarget(
                    selected = value == selected,
                    label = label(value),
                    onClick = { onSelect(value) },
                )
                Text(
                    text = label(value),
                    style = RodiTheme.typography.caption1Medium,
                    color = RodiTheme.colors.gray800,
                    textAlign = TextAlign.Center,
                )
            }
        },
    ) { measurables, constraints ->
        val touchTargets = measurables.filterIndexed { index, _ -> index % 2 == 0 }
            .map { it.measure(Constraints.fixed(ScaleTouchSize.roundToPx(), ScaleTouchSize.roundToPx())) }
        val labels = measurables.filterIndexed { index, _ -> index % 2 == 1 }
            .map { it.measure(Constraints()) }
        val pitchPx = ScalePitch.roundToPx()
        val circleSizePx = ScaleCircleSize.roundToPx()
        val circleOffsetPx = circleOffset.roundToPx()
        val labelTopPx = ScaleLabelTop.roundToPx()
        val contentWidth = labels.mapIndexed { index, placeable ->
            index * pitchPx + circleOffsetPx + circleSizePx / 2 + placeable.width / 2
        }.maxOrNull() ?: 0
        val contentHeight = labelTopPx + (labels.maxOfOrNull { it.height } ?: 0)
        val width = contentWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = contentHeight.coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(width, height) {
            touchTargets.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x = index * pitchPx,
                    y = -16.dp.roundToPx(),
                )
            }
            labels.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x = index * pitchPx + circleOffsetPx + circleSizePx / 2 - placeable.width / 2,
                    y = labelTopPx,
                )
            }
        }
    }
}

@Composable
private fun ScaleTouchTarget(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(ScaleTouchSize)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics {
                contentDescription = label
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(ScaleCircleSize)
                .background(
                    color = if (selected) RodiTheme.colors.primary600 else RodiTheme.colors.white,
                    shape = CircleShape,
                )
                .let {
                    if (selected) it else {
                        it.border(1.dp, RodiTheme.colors.primary300, CircleShape)
                    }
                },
        )
    }
}

private enum class PreviewScale { ONE, TWO, THREE, FOUR, FIVE }

@Preview(name = "눈금 - 5단계 미선택", showBackground = true, widthDp = 375)
@Composable
private fun ScalePickerFiveEmptyPreview() = RodiTheme {
    ScalePicker(PreviewScale.entries, null, PreviewScale::name, {})
}

@Preview(name = "눈금 - 5단계 선택", showBackground = true, widthDp = 375)
@Composable
private fun ScalePickerFiveSelectedPreview() = RodiTheme {
    ScalePicker(PreviewScale.entries, PreviewScale.THREE, PreviewScale::name, {})
}

@Preview(name = "눈금 - 3단계 미선택", showBackground = true, widthDp = 375)
@Composable
private fun ScalePickerThreeEmptyPreview() = RodiTheme {
    ScalePicker(PreviewScale.entries.take(3), null, PreviewScale::name, {})
}

@Preview(name = "눈금 - 3단계 선택", showBackground = true, widthDp = 375)
@Composable
private fun ScalePickerThreeSelectedPreview() = RodiTheme {
    ScalePicker(PreviewScale.entries.take(3), PreviewScale.TWO, PreviewScale::name, {})
}
