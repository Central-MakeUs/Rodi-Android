package com.dororong.rodi.core.data.source.remote.model.member

import kotlinx.serialization.Serializable

@Serializable
data class NicknameAvailabilityResponse(
    val nickname: String,
    val available: Boolean,
    val reason: String? = null,
    val message: String? = null,
)

@Serializable
data class NicknameUpdateRequest(val nickname: String)
