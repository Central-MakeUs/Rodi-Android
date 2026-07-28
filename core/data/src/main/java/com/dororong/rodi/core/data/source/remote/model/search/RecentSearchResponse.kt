package com.dororong.rodi.core.data.source.remote.model.search

import kotlinx.serialization.Serializable

@Serializable
data class RecentSearchResponse(
    val id: Long,
    val keyword: String,
)
