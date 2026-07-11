package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.auth.AuthSession
import com.dororong.rodi.core.domain.model.onboarding.OnboardingProfile

interface AuthRepository {
    suspend fun getSession(): AuthSession

    /** @return isNewMember(이번 로그인으로 신규 가입됐는지) */
    suspend fun loginWithKakao(kakaoAccessToken: String, onboardingProfile: OnboardingProfile? = null): Boolean
}
