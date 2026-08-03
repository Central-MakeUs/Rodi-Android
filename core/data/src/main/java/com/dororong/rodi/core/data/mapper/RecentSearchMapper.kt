package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.search.RecentSearchResponse
import com.dororong.rodi.core.domain.model.search.RecentSearch
import com.dororong.rodi.core.domain.model.search.SearchTargetType

fun RecentSearchResponse.toDomain() = RecentSearch(
    id = id,
    keyword = keyword,
    type = type?.let { targetType ->
        SearchTargetType.entries.firstOrNull { it.name == targetType }
    },
    placeId = placeId,
    regionKey = regionKey,
)
