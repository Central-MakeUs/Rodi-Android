package com.dororong.rodi.core.domain.usecase.member

import com.dororong.rodi.core.domain.model.member.PracticeRecordItem
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.model.practice.PracticeStatus
import com.dororong.rodi.core.domain.repository.MemberRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetPracticeRecordsUseCaseTest {
    private val repository = mockk<MemberRepository>()
    private val useCase = GetPracticeRecordsUseCase(repository)

    @Test
    fun `practice pages contain only visited records and skip invisible pages`() = kotlinx.coroutines.test.runTest {
        coEvery { repository.getPracticeRecords(null, 1) } returns CursorPage(
            items = listOf(record(1L, PracticeStatus.PLANNED)),
            hasNext = true,
            nextCursor = "next",
            totalCount = 2,
        )
        coEvery { repository.getPracticeRecords("next", 1) } returns CursorPage(
            items = listOf(record(2L, PracticeStatus.VISITED)),
            hasNext = false,
            nextCursor = null,
            totalCount = 2,
        )

        val result = useCase(cursor = null, size = 1).getOrThrow()

        assertEquals(listOf(2L), result.items.map(PracticeRecordItem::practiceId))
        assertEquals(false, result.hasNext)
        coVerify(exactly = 1) { repository.getPracticeRecords(null, 1) }
        coVerify(exactly = 1) { repository.getPracticeRecords("next", 1) }
    }

    @Test
    fun `presence follows the repository visited-only contract`() = kotlinx.coroutines.test.runTest {
        coEvery { repository.hasPracticeRecords() } returns true

        assertTrue(useCase.hasAny().getOrThrow())
        coVerify(exactly = 1) { repository.hasPracticeRecords() }
    }

    private fun record(id: Long, status: PracticeStatus) = PracticeRecordItem(
        practiceId = id,
        placeId = id,
        placeName = "장소$id",
        practiceTypes = listOf(PracticeType.ROUNDABOUT),
        visitCount = 1,
        visitedAt = Instant.EPOCH.takeIf { status == PracticeStatus.VISITED },
        isVerified = status == PracticeStatus.VISITED,
        hasReview = false,
        status = status,
    )
}
