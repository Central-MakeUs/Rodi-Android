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
    fun `semantic duplicate from another source is moved to the front without growing history`() {
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
    fun `unresolved suggestions cannot enter search history`() {
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
