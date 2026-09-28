package com.dororong.rodi.feature.home

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HomeSheetValueMappingTest {

    @Test
    fun `멈춘 시트 값을 화면 상태로 그대로 매핑한다`() {
        assertEquals(HomeSurfaceState.Navigation, ListSheetValue.Hidden.toSurfaceState())
        assertEquals(HomeSurfaceState.PartialList, ListSheetValue.Partial.toSurfaceState())
        assertEquals(HomeSurfaceState.FullList, ListSheetValue.Full.toSurfaceState())
    }

    @Test
    fun `상세와 지도 상태에서는 목록 시트를 숨긴다`() {
        assertEquals(ListSheetValue.Hidden, HomeSurfaceState.Detail.toListSheetValue(allowFull = true))
        assertEquals(ListSheetValue.Hidden, HomeSurfaceState.Navigation.toListSheetValue(allowFull = true))
    }

    @Test
    fun `펼침 위치가 없으면 전체 목록을 부분 펼침으로 대신한다`() {
        assertEquals(ListSheetValue.Full, HomeSurfaceState.FullList.toListSheetValue(allowFull = true))
        assertEquals(ListSheetValue.Partial, HomeSurfaceState.FullList.toListSheetValue(allowFull = false))
    }
}
