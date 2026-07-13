package com.dororong.rodi.core.data.source.remote.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class SocialLoginResponse(
    val status: String,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val isNewMember: Boolean? = null,
    val nickname: String? = null,
    val withdrawalRequestedAt: String? = null,
    val recoverableUntil: String? = null,
)
