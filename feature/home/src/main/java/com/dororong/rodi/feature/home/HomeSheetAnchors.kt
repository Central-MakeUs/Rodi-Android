package com.dororong.rodi.feature.home

import androidx.compose.foundation.gestures.DraggableAnchors

enum class ListSheetValue {
    Hidden,
    Partial,
    Full,
}

enum class DetailSheetValue {
    Visible,
    Dismissed,
}

internal data class ListSheetAnchorPositions(
    val hiddenPx: Float,
    val partialPx: Float,
    val fullPx: Float?,
)

internal object ListSheetAnchorPolicy {

    fun positions(
        containerHeightPx: Int,
        peekHeightPx: Float,
        allowFull: Boolean,
    ): ListSheetAnchorPositions {
        val height = containerHeightPx.coerceAtLeast(0).toFloat()
        return ListSheetAnchorPositions(
            hiddenPx = height,
            partialPx = (height - peekHeightPx).coerceIn(0f, height),
            fullPx = if (allowFull) 0f else null,
        )
    }

    /** Partial(0f) → Full(1f) 구간의 진행도. 헤더 높이·모서리 라운딩·타이틀 정렬이 이 값을 따라간다. */
    fun expansionProgress(offsetPx: Float, partialOffsetPx: Float): Float =
        if (partialOffsetPx <= 0f) {
            1f
        } else {
            ((partialOffsetPx - offsetPx) / partialOffsetPx).coerceIn(0f, 1f)
        }
}

internal fun listSheetAnchors(
    containerHeightPx: Int,
    peekHeightPx: Float,
    allowFull: Boolean,
): DraggableAnchors<ListSheetValue> {
    val positions = ListSheetAnchorPolicy.positions(containerHeightPx, peekHeightPx, allowFull)
    return DraggableAnchors {
        ListSheetValue.Hidden at positions.hiddenPx
        ListSheetValue.Partial at positions.partialPx
        positions.fullPx?.let { ListSheetValue.Full at it }
    }
}

internal fun detailSheetAnchors(sheetHeightPx: Int): DraggableAnchors<DetailSheetValue> =
    DraggableAnchors {
        DetailSheetValue.Visible at 0f
        DetailSheetValue.Dismissed at sheetHeightPx.coerceAtLeast(0).toFloat()
    }

internal fun ListSheetValue.toSurfaceState(): HomeSurfaceState = when (this) {
    ListSheetValue.Hidden -> HomeSurfaceState.Navigation
    ListSheetValue.Partial -> HomeSurfaceState.PartialList
    ListSheetValue.Full -> HomeSurfaceState.FullList
}

/**
 * [allowFull]이 false면 FullList도 Partial로 접는다. 앵커에 없는 값으로 animateTo 하면
 * offset은 그대로인 채 currentValue만 바뀌어 상태와 화면이 어긋난다.
 */
internal fun HomeSurfaceState.toListSheetValue(allowFull: Boolean): ListSheetValue = when (this) {
    HomeSurfaceState.Navigation, HomeSurfaceState.Detail -> ListSheetValue.Hidden
    HomeSurfaceState.PartialList -> ListSheetValue.Partial
    HomeSurfaceState.FullList -> if (allowFull) ListSheetValue.Full else ListSheetValue.Partial
}
