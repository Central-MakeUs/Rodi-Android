package com.dororong.rodi.feature.home.detail.components

import com.dororong.rodi.core.domain.model.place.ParkingFeeInfo
import com.dororong.rodi.core.domain.model.place.ParkingOperatingHours
import com.dororong.rodi.core.domain.model.place.ParkingPlaceDetail
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ParkingFeeDisplayTest {

    @Test
    fun `paid parking keeps only the base and additional rates`() {
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
    fun `free parking keeps two rows and marks the base rate free`() {
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
