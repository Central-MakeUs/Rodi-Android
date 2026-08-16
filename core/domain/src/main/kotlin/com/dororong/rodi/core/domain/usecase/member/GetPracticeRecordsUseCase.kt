package com.dororong.rodi.core.domain.usecase.member

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.member.PracticeRecordItem
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.practice.PracticeStatus
import com.dororong.rodi.core.domain.repository.MemberRepository
import javax.inject.Inject

class GetPracticeRecordsUseCase @Inject constructor(private val repository: MemberRepository) {
    suspend operator fun invoke(cursor: String? = null, size: Int = 20): Result<CursorPage<PracticeRecordItem>> =
        runSuspendCatching {
            require(size > 0) { "연습기록 페이지 크기는 1 이상이어야 합니다." }

            val visibleItems = mutableListOf<PracticeRecordItem>()
            var requestCursor = cursor
            var requestSize = size
            var page: CursorPage<PracticeRecordItem>
            var requestedPageCount = 0

            do {
                page = repository.getPracticeRecords(requestCursor, requestSize)
                requestedPageCount++
                visibleItems += page.items.filter { it.status == PracticeStatus.VISITED }

                val nextCursor = page.nextCursor
                val canRequestNextPage = page.hasNext &&
                    nextCursor != null &&
                    nextCursor != requestCursor
                if (visibleItems.size >= size || !canRequestNextPage || requestedPageCount >= MAX_PAGE_REQUESTS) break
                requestCursor = nextCursor
                requestSize = (size - visibleItems.size).coerceAtLeast(1)
            } while (true)

            CursorPage(
                items = visibleItems,
                hasNext = page.hasNext &&
                    page.nextCursor != null &&
                    page.nextCursor != requestCursor,
                nextCursor = page.nextCursor,
                totalCount = page.totalCount,
            )
    }

    private companion object {
        const val MAX_PAGE_REQUESTS = 5
    }
}
