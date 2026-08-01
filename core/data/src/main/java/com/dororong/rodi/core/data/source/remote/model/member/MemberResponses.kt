package com.dororong.rodi.core.data.source.remote.model.member

import kotlinx.serialization.Serializable

@Serializable
data class MyPageResponse(
    val nickname: String,
    val level: String,
    val recommendationTags: List<String> = emptyList(),
    val drivingGoal: String? = null,
    val savedPlaceCount: Long = 0,
)

@Serializable
data class MemberUpdateRequest(
    val drivingGoal: String,
)

@Serializable
data class FilterTagsRequest(
    val filterTags: List<String>,
)
