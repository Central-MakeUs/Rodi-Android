package com.dororong.rodi.core.data.source.local.security

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val provider: String,
) {
    val isKakaoProvider: Boolean get() = provider == KAKAO_PROVIDER
}

const val KAKAO_PROVIDER = "kakao"
