package com.dororong.rodi.core.data.source.local.datastore

import com.dororong.rodi.core.domain.model.course.CourseLocationKind
import com.dororong.rodi.core.domain.model.course.CourseLocationSuggestion
import com.dororong.rodi.core.domain.model.course.CourseLocationSuggestionSource
import com.dororong.rodi.core.domain.model.course.GeoPoint
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CourseSearchHistoryDataStoreTest {
    @Test
    fun `다른 출처의 같은 검색어는 기록을 늘리지 않고 맨 앞으로 옮긴다`() {
        val current = (0 until CourseSearchHistoryDataStore.MAX_HISTORY_SIZE).map { index ->
            CourseLocationSuggestion(
                id = "history-$index",
                title = "장소 $index",
                address = "주소 $index",
                point = GeoPoint(37.0 + index * 0.01, 127.0 + index * 0.01),
                kind = CourseLocationKind.PLACE,
                lastUsedAt = Instant.ofEpochSecond(index.toLong()),
            )
        }
        val replacement = CourseLocationSuggestion(
            id = "server-place-0",
            title = "장소 0",
            address = "주소 0",
            point = GeoPoint(37.000004, 127.000004),
            kind = CourseLocationKind.PLACE,
            source = CourseLocationSuggestionSource.SERVER_PLACE,
        )

        val updated = mergeCourseSearchHistory(
            current = current,
            suggestion = replacement,
            usedAt = Instant.ofEpochSecond(100),
        )

        assertEquals(CourseSearchHistoryDataStore.MAX_HISTORY_SIZE, updated.size)
        assertEquals("server-place-0", updated.first().id)
        assertEquals(Instant.ofEpochSecond(100), updated.first().lastUsedAt)
        assertEquals((1 until CourseSearchHistoryDataStore.MAX_HISTORY_SIZE).map { "history-$it" }, updated.drop(1).map { it.id })
    }

    @Test
    fun `이전에 저장된 서버 검색 기록은 최근 검색어에서 숨긴다`() {
        val stored = listOf(
            suggestion("server-region-1", CourseLocationSuggestionSource.SERVER_REGION),
            suggestion("server-place-2", CourseLocationSuggestionSource.SERVER_PLACE),
            suggestion("kakao-keyword-3", CourseLocationSuggestionSource.KAKAO_KEYWORD),
            suggestion("kakao-address-4", CourseLocationSuggestionSource.KAKAO_ADDRESS),
            suggestion("reverse-5", CourseLocationSuggestionSource.REVERSE_GEOCODE),
            suggestion("history-6", CourseLocationSuggestionSource.HISTORY),
        )

        val visible = stored.excludeServerSourced()

        assertEquals(
            listOf("kakao-keyword-3", "kakao-address-4", "reverse-5", "history-6"),
            visible.map { it.id },
        )
    }

    private fun suggestion(id: String, source: CourseLocationSuggestionSource) = CourseLocationSuggestion(
        id = id,
        title = "장소",
        address = "주소",
        point = GeoPoint(37.0, 127.0),
        kind = CourseLocationKind.PLACE,
        source = source,
    )

    @Test
    fun `확정되지 않은 검색 결과는 검색 기록에 넣을 수 없다`() {
        val unresolved = CourseLocationSuggestion(
            id = "server-place-7",
            title = "장소",
            address = "주소",
            point = null,
            kind = CourseLocationKind.PLACE,
        )

        assertThrows(IllegalArgumentException::class.java) {
            mergeCourseSearchHistory(emptyList(), unresolved)
        }
    }
}
