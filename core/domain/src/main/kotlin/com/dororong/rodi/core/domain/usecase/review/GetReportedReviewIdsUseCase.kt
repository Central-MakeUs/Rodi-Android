package com.dororong.rodi.core.domain.usecase.review

import com.dororong.rodi.core.domain.repository.ReviewRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class GetReportedReviewIdsUseCase @Inject constructor(
    private val repository: ReviewRepository,
) {
    suspend operator fun invoke(): Set<Long> = try {
        repository.getReportedReviewIds()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        emptySet()
    }
}
