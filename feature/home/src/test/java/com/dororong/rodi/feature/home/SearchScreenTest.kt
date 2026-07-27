package com.dororong.rodi.feature.home

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SearchScreenTest {

    @Test
    fun `중구 입력 시 연관 지역구를 표시한다`() {
        assertEquals(
            listOf("서울 중구", "대전 중구", "울산 중구", "부산 중구", "대구 중구"),
            regionSuggestionsFor("중구"),
        )
    }

    @Test
    fun `일치하는 지역구가 없으면 빈 목록을 반환한다`() {
        assertEquals(emptyList<String>(), regionSuggestionsFor("중군"))
    }
}
