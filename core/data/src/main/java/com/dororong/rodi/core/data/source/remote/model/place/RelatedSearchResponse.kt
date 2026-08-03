package com.dororong.rodi.core.data.source.remote.model.place

import kotlinx.serialization.Serializable

@Serializable
data class RelatedSearchResponse(
    val regions: List<String>,
    val places: CursorPagePlaceSuggestionResponse,
)

@Serializable
data class CursorPagePlaceSuggestionResponse(
    val items: List<PlaceSuggestionResponse>,
    val hasNext: Boolean,
    val nextCursor: String? = null,
    val totalCount: Long? = null,
)

@Serializable
data class PlaceSuggestionResponse(
    val placeId: Long,
    val name: String,
    val region: String,
)
