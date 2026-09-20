package com.dororong.rodi.core.data.source.local.security

import java.util.UUID

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val provider: String,
    val isCourseTutorialCompleted: Boolean = false,
    // 프로세스 내 요청 소유권이다. 디스크에 저장하지 않으며 token rotation에서는 유지한다.
    val sessionId: String = UUID.randomUUID().toString(),
) {
    val isKakaoProvider: Boolean get() = provider == KAKAO_PROVIDER
}

const val KAKAO_PROVIDER = "kakao"
