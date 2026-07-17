package com.dororong.rodi.core.domain.usecase.place

import com.dororong.rodi.core.common.runSuspendCatching
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.repository.PlaceRepository
import javax.inject.Inject

class SetPlaceBookmarkUseCase @Inject constructor(
    private val repository: PlaceRepository,
) {
    suspend operator fun invoke(place: PlaceDetail, bookmarked: Boolean) =
        runSuspendCatching { repository.setBookmarked(place, bookmarked) }
}
