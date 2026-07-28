package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.auth.AuthSession
import com.dororong.rodi.core.domain.model.auth.AccountRestoreResult
import com.dororong.rodi.core.domain.model.auth.LoginResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun getSession(): AuthSession

    suspend fun loginWithKakao(kakaoAccessToken: String): LoginResult

    suspend fun reissueToken()

    fun observeSessionExpiration(): Flow<Unit>

    suspend fun restoreWithKakao(credential: String): AccountRestoreResult

    suspend fun logout()
}
