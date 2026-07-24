package com.dororong.rodi.feature.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DataSourceScreenTest {
    @Test
    fun `parking data source notice keeps the approved attribution`() {
        assertEquals(
            """
                주차장 정보는 공공데이터포털에서 제공하는
                「전국주차장정보표준데이터」를 기반으로 가공되었습니다.

                - 데이터명: 전국주차장정보표준데이터
                - 출처: 공공데이터포털
                - 제공기관: 데이터 상세페이지에 기재된 제공기관
                - 데이터 기준일: 2026년 7월 23일

                본 서비스에서 제공하는 정보는 원본 데이터를 가공한 것으로,
                실제 주차장 운영시간, 요금 및 운영 상태와 차이가 있을 수 있습니다.
            """.trimIndent(),
            ParkingDataSourceNotice,
        )
    }
}
