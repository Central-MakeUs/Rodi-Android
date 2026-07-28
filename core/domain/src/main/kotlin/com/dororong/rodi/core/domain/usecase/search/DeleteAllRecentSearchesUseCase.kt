package com.dororong.rodi.core.domain.usecase.search

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.repository.RecentSearchRepository
import javax.inject.Inject

class DeleteAllRecentSearchesUseCase @Inject constructor(
    private val repository: RecentSearchRepository,
) {
    suspend operator fun invoke() = runSuspendCatching { repository.deleteAllRecentSearches() }
}
