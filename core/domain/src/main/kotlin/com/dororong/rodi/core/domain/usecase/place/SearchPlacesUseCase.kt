package com.dororong.rodi.core.domain.usecase.place

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.course.GeoPoint
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
        val normalizedKeyword = keyword.trim()
        require(normalizedKeyword.length in 1..50) { "검색어는 1~50자여야 합니다." }
        repository.searchPlaces(normalizedKeyword, origin, cursor, size)
    }
}
