package com.dororong.rodi.core.domain

interface AuthRepository {
    /** @return isNewMember(이번 로그인으로 신규 가입됐는지) */
    suspend fun loginWithKakao(kakaoAccessToken: String): Boolean
}
