package com.dororong.rodi.core.domain.model.search

import com.dororong.rodi.core.domain.model.place.CursorPage

data class RelatedSearch(
    val regions: List<String>,
    val places: CursorPage<PlaceSuggestion>,
)

data class PlaceSuggestion(
    val placeId: Long,
    val name: String,
    val region: String,
)

data class RecentSearchRegistration(
    val type: SearchTargetType,
    val keyword: String,
    val placeId: Long? = null,
)
