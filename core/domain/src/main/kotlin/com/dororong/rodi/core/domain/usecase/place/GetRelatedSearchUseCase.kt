package com.dororong.rodi.core.domain.usecase.place

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.search.normalizeSearchKeyword
import com.dororong.rodi.core.domain.repository.PlaceRepository
import javax.inject.Inject

class GetRelatedSearchUseCase @Inject constructor(
    private val repository: PlaceRepository,
) {
    suspend operator fun invoke(
        keyword: String,
        cursor: String? = null,
        size: Int = 20,
    ) = runSuspendCatching {
        val normalizedKeyword = keyword.normalizeSearchKeyword()
        repository.relatedSearch(normalizedKeyword, cursor, size)
    }
}
