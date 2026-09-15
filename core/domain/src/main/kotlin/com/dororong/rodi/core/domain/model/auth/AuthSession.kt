package com.dororong.rodi.core.domain.model.auth

data class AuthSession(
    val isLoggedIn: Boolean,
    val hasRecentKakaoLogin: Boolean,
    val isCourseTutorialCompleted: Boolean = false,
)
