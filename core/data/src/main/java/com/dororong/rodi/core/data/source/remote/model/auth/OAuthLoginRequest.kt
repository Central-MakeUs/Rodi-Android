package com.dororong.rodi.core.data.source.remote.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class OAuthLoginRequest(
    val credential: String,
)
