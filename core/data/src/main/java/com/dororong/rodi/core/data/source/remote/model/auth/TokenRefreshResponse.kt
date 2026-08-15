package com.dororong.rodi.core.data.source.remote.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class TokenRefreshResponse(
    val accessToken: String,
    val refreshToken: String,
    val isOnboarded: Boolean,
    val isCourseTutorialCompleted: Boolean,
)
