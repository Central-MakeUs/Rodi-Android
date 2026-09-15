package com.dororong.rodi.core.domain.usecase.review

import com.dororong.rodi.core.domain.repository.ReviewRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class GetReportedReviewIdsUseCase @Inject constructor(
    private val repository: ReviewRepository,
) {
    /**
     * 조회 실패는 빈 집합으로 삼킨다 — 이 목록은 이미 신고한 후기를 신고자 화면에서만 가리는
     * 보조 필터라, 읽기가 실패했다고 코스 상세 전체를 오류 화면으로 막는 쪽이 더 나쁘다.
     * 다만 `Error`(OOM 등)까지 삼키지는 않는다.
     */
    suspend operator fun invoke(): Set<Long> = try {
        repository.getReportedReviewIds()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        emptySet()
    }
}
