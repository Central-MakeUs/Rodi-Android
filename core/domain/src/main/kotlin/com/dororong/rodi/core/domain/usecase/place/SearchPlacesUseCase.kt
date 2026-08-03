package com.dororong.rodi.core.domain.usecase.place

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.search.normalizeSearchKeyword
import com.dororong.rodi.core.domain.repository.PlaceRepository
import javax.inject.Inject

class SearchPlacesUseCase @Inject constructor(
    private val repository: PlaceRepository,
) {
    suspend operator fun invoke(
        keyword: String,
        origin: GeoPoint,
        cursor: String? = null,
        size: Int = 20,
    ) = runSuspendCatching {
        val normalizedKeyword = keyword.normalizeSearchKeyword()
        repository.searchPlaces(normalizedKeyword, origin, cursor, size)
    }
}
