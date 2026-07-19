package com.dororong.rodi.feature.home

internal data class ParkingSheetLayoutState(
    val placeId: Long? = null,
    val currentHeightPx: Int = 0,
    val initialMapPaddingPx: Int? = null,
) {
    fun forPlace(placeId: Long?): ParkingSheetLayoutState =
        if (this.placeId == placeId) this else ParkingSheetLayoutState(placeId = placeId)

    fun onMeasured(placeId: Long, heightPx: Int): ParkingSheetLayoutState {
        if (this.placeId != placeId || heightPx <= 0) return this
        return copy(
            currentHeightPx = heightPx,
            initialMapPaddingPx = initialMapPaddingPx ?: heightPx,
        )
    }
}
