package com.dororong.rodi.feature.mypage.practicerecords

import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.model.practice.PracticeStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class PracticeRecordTest {
    @Test
    fun `후기가 없는 코스는 후기 작성을 열 수 있다`() {
        val action = courseRecord().reviewAction

        assertEquals(PracticeRecordReviewAction.WRITE_REVIEW, action)
        assertTrue(action.isEnabled)
        assertEquals("후기 작성", action.label)
    }

    @Test
    fun `후기가 있는 코스는 작성 완료로 표시하고 후기 작성을 열 수 없다`() {
        val action = courseRecord().copy(hasReview = true).reviewAction

        assertEquals(PracticeRecordReviewAction.REVIEW_COMPLETED, action)
        assertFalse(action.isEnabled)
        assertEquals("작성 완료", action.label)
    }

    @Test
    fun `주차장 기록은 후기가 없어도 작성할 수 없다`() {
        val action = courseRecord()
            .copy(practiceTypes = listOf(PracticeType.PARKING))
            .reviewAction

        assertEquals(PracticeRecordReviewAction.PARKING_UNAVAILABLE, action)
        assertFalse(action.isEnabled)
        assertEquals("작성 불가", action.label)
    }

    @Test
    fun `방문 기록에 날짜가 있으면 운전 날짜를 보여준다`() {
        val dateLabel = courseRecord()
            .copy(visitedAt = Instant.parse("2026-05-10T12:00:00Z"))
            .visitedDateLabel()

        assertTrue(dateLabel.matches(Regex("\\d{2}\\.\\d{2}\\.\\d{2}")))
    }

    @Test
    fun `방문 기록에 시각이 없으면 방문 상태로 대신한다`() {
        val dateLabel = courseRecord().copy(visitedAt = null).visitedDateLabel()

        assertEquals("방문 완료", dateLabel)
    }

    @Test
    fun `방문하지 않은 기록에 시각이 없으면 아무것도 보여주지 않는다`() {
        val dateLabel = courseRecord()
            .copy(visitedAt = null, status = PracticeStatus.NOT_VISITED)
            .visitedDateLabel()

        assertEquals("", dateLabel)
    }

    private fun courseRecord() = PracticeRecord(
        practiceId = 1L,
        placeId = 1L,
        placeName = "코스",
        practiceTypes = listOf(PracticeType.ROUNDABOUT),
        visitCount = 1,
        visitedAt = java.time.Instant.EPOCH,
        hasReview = false,
        status = PracticeStatus.VISITED,
    )
}
