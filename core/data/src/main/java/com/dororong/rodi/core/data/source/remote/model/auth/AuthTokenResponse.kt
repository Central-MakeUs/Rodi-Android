package com.dororong.rodi.core.data.source.remote.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val isNewMember: Boolean,
)
