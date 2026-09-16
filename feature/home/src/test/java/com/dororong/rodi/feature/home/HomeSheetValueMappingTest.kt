package com.dororong.rodi.feature.home

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HomeSheetValueMappingTest {

    @Test
    fun `settled sheet value maps straight onto the surface state`() {
        assertEquals(HomeSurfaceState.Navigation, ListSheetValue.Hidden.toSurfaceState())
        assertEquals(HomeSurfaceState.PartialList, ListSheetValue.Partial.toSurfaceState())
        assertEquals(HomeSurfaceState.FullList, ListSheetValue.Full.toSurfaceState())
    }

    @Test
    fun `detail hides the list sheet`() {
        assertEquals(ListSheetValue.Hidden, HomeSurfaceState.Detail.toListSheetValue(allowFull = true))
        assertEquals(ListSheetValue.Hidden, HomeSurfaceState.Navigation.toListSheetValue(allowFull = true))
    }

    @Test
    fun `full list falls back to partial when the full anchor is missing`() {
        assertEquals(ListSheetValue.Full, HomeSurfaceState.FullList.toListSheetValue(allowFull = true))
        assertEquals(ListSheetValue.Partial, HomeSurfaceState.FullList.toListSheetValue(allowFull = false))
    }
}
