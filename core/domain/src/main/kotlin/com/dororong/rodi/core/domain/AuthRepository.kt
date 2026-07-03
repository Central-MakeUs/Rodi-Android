package com.dororong.rodi.core.domain

interface AuthRepository {
    suspend fun loginWithKakao(kakaoAccessToken: String)
}
