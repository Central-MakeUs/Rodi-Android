package com.dororong.rodi.feature.home

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ParkingSheetLayoutStateTest {
    @Test
    fun `같은 주차장의 토글 높이는 현재 높이만 바꾸고 지도 패딩은 유지한다`() {
        val initial = ParkingSheetLayoutState()
            .forPlace(1L)
            .onMeasured(placeId = 1L, heightPx = 240)

        val toggled = initial.onMeasured(placeId = 1L, heightPx = 400)

        assertEquals(400, toggled.currentHeightPx)
        assertEquals(240, toggled.initialMapPaddingPx)
    }

    @Test
    fun `새 주차장은 측정값을 초기화하고 자신의 최초 높이를 사용한다`() {
        val firstPlace = ParkingSheetLayoutState()
            .forPlace(1L)
            .onMeasured(placeId = 1L, heightPx = 240)

        val secondPlace = firstPlace
            .forPlace(2L)
            .onMeasured(placeId = 2L, heightPx = 360)

        assertEquals(2L, secondPlace.placeId)
        assertEquals(360, secondPlace.currentHeightPx)
        assertEquals(360, secondPlace.initialMapPaddingPx)
    }

    @Test
    fun `이전 주차장의 늦은 측정은 새 주차장 상태를 바꾸지 않는다`() {
        val secondPlace = ParkingSheetLayoutState().forPlace(2L)

        assertEquals(secondPlace, secondPlace.onMeasured(placeId = 1L, heightPx = 400))
    }
}
