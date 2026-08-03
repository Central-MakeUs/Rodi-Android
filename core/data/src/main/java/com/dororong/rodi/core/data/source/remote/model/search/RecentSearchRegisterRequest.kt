package com.dororong.rodi.core.data.source.remote.model.search

import kotlinx.serialization.Serializable

@Serializable
data class RecentSearchRegisterRequest(
    val type: String,
    val keyword: String,
    val placeId: Long? = null,
)
