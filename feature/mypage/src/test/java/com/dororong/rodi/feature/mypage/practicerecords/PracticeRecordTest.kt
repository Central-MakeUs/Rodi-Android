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
    fun `course without a review can open review writing`() {
        val action = courseRecord().reviewAction

        assertEquals(PracticeRecordReviewAction.WRITE_REVIEW, action)
        assertTrue(action.isEnabled)
        assertEquals("후기 작성", action.label)
    }

    @Test
    fun `course with a review is marked completed and cannot open review writing`() {
        val action = courseRecord().copy(hasReview = true).reviewAction

        assertEquals(PracticeRecordReviewAction.REVIEW_COMPLETED, action)
        assertFalse(action.isEnabled)
        assertEquals("작성 완료", action.label)
    }

    @Test
    fun `parking record is not writable even without a review`() {
        val action = courseRecord()
            .copy(practiceTypes = listOf(PracticeType.PARKING))
            .reviewAction

        assertEquals(PracticeRecordReviewAction.PARKING_UNAVAILABLE, action)
        assertFalse(action.isEnabled)
        assertEquals("작성 불가", action.label)
    }

    @Test
    fun `visited record exposes the driving date when it has one`() {
        val dateLabel = courseRecord()
            .copy(visitedAt = Instant.parse("2026-05-10T12:00:00Z"))
            .visitedDateLabel()

        assertTrue(dateLabel.matches(Regex("\\d{2}\\.\\d{2}\\.\\d{2}")))
    }

    @Test
    fun `visited record without a timestamp falls back to visit status`() {
        val dateLabel = courseRecord().copy(visitedAt = null).visitedDateLabel()

        assertEquals("방문 완료", dateLabel)
    }

    @Test
    fun `non-visited record without a timestamp shows nothing`() {
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
        isVerified = true,
        hasReview = false,
        status = PracticeStatus.VISITED,
    )
}
