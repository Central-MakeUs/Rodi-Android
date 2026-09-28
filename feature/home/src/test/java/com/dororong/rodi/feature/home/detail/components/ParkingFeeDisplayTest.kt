package com.dororong.rodi.feature.home.detail.components

import com.dororong.rodi.core.domain.model.place.ParkingFeeInfo
import com.dororong.rodi.core.domain.model.place.ParkingOperatingHours
import com.dororong.rodi.core.domain.model.place.ParkingPlaceDetail
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ParkingFeeDisplayTest {

    @Test
    fun `유료 주차장은 기본 요금과 추가 요금만 남긴다`() {
        val rows = parking(
            isFree = false,
            feeInfo = ParkingFeeInfo(
                baseMinutes = 60,
                baseFee = 2_800,
                addUnitMinutes = 10,
                addUnitFee = 1_000,
                dayTicketHours = null,
                dayTicketFee = null,
                monthlyFee = null,
            ),
        ).toFeeDisplayRows()

        assertEquals(
            listOf(
                ParkingFeeDisplayRow("기본요금", "60분 ･ 2,800원"),
                ParkingFeeDisplayRow("추가요금", "10분 ･ 1,000원"),
            ),
            rows,
        )
    }

    @Test
    fun `무료 주차장은 두 줄을 유지하고 기본 요금을 무료로 표시한다`() {
        val rows = parking(isFree = true, feeInfo = null).toFeeDisplayRows()

        assertEquals(
            listOf(
                ParkingFeeDisplayRow("기본요금", "무료"),
                ParkingFeeDisplayRow("추가요금", "해당항목없음"),
            ),
            rows,
        )
    }

    private fun parking(
        isFree: Boolean,
        feeInfo: ParkingFeeInfo?,
    ) = ParkingPlaceDetail(
        roadAddress = null,
        lotAddress = null,
        managementNo = null,
        parkingType = null,
        capacity = null,
        isFree = isFree,
        feeInfo = feeInfo,
        operatingHours = ParkingOperatingHours(null, null, null),
    )
}
