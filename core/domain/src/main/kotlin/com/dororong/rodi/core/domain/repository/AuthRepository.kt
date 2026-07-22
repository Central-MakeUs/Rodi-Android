package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.auth.AuthSession
import com.dororong.rodi.core.domain.model.auth.AccountRestoreResult
import com.dororong.rodi.core.domain.model.auth.LoginResult

interface AuthRepository {
    suspend fun getSession(): AuthSession

    suspend fun loginWithKakao(kakaoAccessToken: String): LoginResult

    suspend fun reissueToken()

    suspend fun restoreWithKakao(credential: String): AccountRestoreResult

    suspend fun logout()
}
