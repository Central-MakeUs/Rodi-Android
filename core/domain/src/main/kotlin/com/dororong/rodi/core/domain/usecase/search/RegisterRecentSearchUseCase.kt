package com.dororong.rodi.core.domain.usecase.search

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.search.RecentSearchRegistration
import com.dororong.rodi.core.domain.model.search.SearchTargetType
import com.dororong.rodi.core.domain.repository.RecentSearchRepository
import javax.inject.Inject

class RegisterRecentSearchUseCase @Inject constructor(
    private val repository: RecentSearchRepository,
) {
    suspend operator fun invoke(search: RecentSearchRegistration) = runSuspendCatching {
        val normalized = search.copy(keyword = search.keyword.trim())
        require(normalized.keyword.length in 1..100) { "검색어는 1~100자여야 합니다." }
        require(normalized.type != SearchTargetType.PLACE || normalized.placeId != null) {
            "장소 검색어에는 placeId가 필요합니다."
        }
        repository.registerRecentSearch(normalized)
    }
}
