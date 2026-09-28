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
    fun `연습 기록 페이지는 방문 기록만 담고 보이는 기록이 없는 페이지는 건너뛴다`() = kotlinx.coroutines.test.runTest {
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
    fun `보이는 기록이 없으면 페이지 탐색을 정해진 횟수까지만 한다`() = kotlinx.coroutines.test.runTest {
        coEvery { repository.getPracticeRecords(null, 1) } returns pageFor(null, "cursor-1")
        coEvery { repository.getPracticeRecords("cursor-1", 1) } returns pageFor("cursor-1", "cursor-2")
        coEvery { repository.getPracticeRecords("cursor-2", 1) } returns pageFor("cursor-2", "cursor-3")
        coEvery { repository.getPracticeRecords("cursor-3", 1) } returns pageFor("cursor-3", "cursor-4")
        coEvery { repository.getPracticeRecords("cursor-4", 1) } returns pageFor("cursor-4", "cursor-5")

        val result = useCase(cursor = null, size = 1).getOrThrow()

        assertEquals(emptyList<PracticeRecordItem>(), result.items)
        assertTrue(result.hasNext)
        assertEquals("cursor-5", result.nextCursor)
        coVerify(exactly = 1) { repository.getPracticeRecords(null, 1) }
        coVerify(exactly = 1) { repository.getPracticeRecords("cursor-4", 1) }
        coVerify(exactly = 0) { repository.getPracticeRecords("cursor-5", 1) }
    }

    private fun record(id: Long, status: PracticeStatus) = PracticeRecordItem(
        practiceId = id,
        placeId = id,
        placeName = "장소$id",
        practiceTypes = listOf(PracticeType.ROUNDABOUT),
        visitCount = 1,
        visitedAt = Instant.EPOCH.takeIf { status == PracticeStatus.VISITED },
        hasReview = false,
        status = status,
    )

    private fun pageFor(cursor: String?, nextCursor: String) = CursorPage(
        items = listOf(record((cursor?.removePrefix("cursor-")?.toLongOrNull() ?: 0L) + 1, PracticeStatus.PLANNED)),
        hasNext = true,
        nextCursor = nextCursor,
        totalCount = 10,
    )
}
