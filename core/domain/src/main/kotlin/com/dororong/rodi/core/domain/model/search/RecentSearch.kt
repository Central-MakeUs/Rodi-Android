package com.dororong.rodi.core.domain.model.search

enum class SearchTargetType {
    REGION,
    PLACE,
}

data class RecentSearch(
    val id: Long,
    val keyword: String,
    val type: SearchTargetType? = null,
    val placeId: Long? = null,
    val regionKey: String? = null,
)
