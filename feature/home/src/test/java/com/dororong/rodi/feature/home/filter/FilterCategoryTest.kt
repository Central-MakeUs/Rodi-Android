package com.dororong.rodi.feature.home.filter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FilterCategoryTest {
    @Test
    fun `카테고리마다 피그마에 정의된 연습유형만 노출한다`() {
        assertEquals(
            listOf("직선주행", "좌우회전", "차선변경", "전체"),
            FilterCategory.BASIC_DRIVING.practiceOptions.map(FilterPracticeOption::label),
        )
        assertEquals(
            listOf("교차로", "유턴", "전체"),
            FilterCategory.URBAN_BASICS.practiceOptions.map(FilterPracticeOption::label),
        )
        assertTrue(FilterCategory.PARKING.practiceOptions.isEmpty())
        assertEquals(
            listOf("다차로주행", "합류", "고속진입", "전체"),
            FilterCategory.ROAD_FLOW.practiceOptions.map(FilterPracticeOption::label),
        )
        assertEquals(
            listOf("회전교차로", "비보호좌회전", "좁은도로", "코너링", "전체"),
            FilterCategory.COMPLEX_SITUATIONS.practiceOptions.map(FilterPracticeOption::label),
        )
    }
}
