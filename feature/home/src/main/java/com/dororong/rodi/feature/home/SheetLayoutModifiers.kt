package com.dororong.rodi.feature.home

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Density
import kotlin.math.roundToInt

/**
 * 시트 오프셋에서 파생된 높이를 측정 단계에서 읽는다.
 *
 * 드래그는 프레임마다 오프셋을 바꾸므로 `Modifier.height(offsetFromState)`처럼 컴포지션에서 읽으면
 * 화면 전체가 매 프레임 recompose 된다. [height] 람다를 measure 블록 안에서 호출하면 스냅샷 읽기가
 * 레이아웃 단계에 등록되어 측정만 다시 돈다.
 */
internal fun Modifier.layoutHeightPx(height: Density.() -> Float): Modifier =
    layout { measurable, constraints ->
        val resolved = height().roundToInt().coerceIn(0, constraints.maxHeight)
        val placeable = measurable.measure(
            constraints.copy(minHeight = resolved, maxHeight = resolved),
        )
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
