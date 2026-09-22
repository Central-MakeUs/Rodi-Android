package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.auth.AuthSession
import com.dororong.rodi.core.domain.model.auth.AccountRestoreResult
import com.dororong.rodi.core.domain.model.auth.LoginResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun getSession(): AuthSession

    suspend fun loginWithKakao(kakaoAccessToken: String): LoginResult

    suspend fun reissueToken(expectedSessionId: String? = null, expectedAccessToken: String? = null)

    /** 서버가 거부해 끝난 세션. 새 로그인 전까지 유지되어 늦게 구독한 쪽도 만료를 알 수 있다. */
    fun observeSessionExpiration(): Flow<Boolean>

    /**
     * 사용자가 로그아웃·탈퇴·삭제로 끝낸 세션. 로컬 정리가 끝난 뒤 한 번 발행하고 보관하지 않는다 —
     * 앱을 다시 열면 저장된 토큰으로 판단하므로 지난 종료를 다시 전달할 필요가 없다.
     */
    fun observeSignOut(): Flow<Unit>

    suspend fun restoreWithKakao(credential: String): AccountRestoreResult

    suspend fun logout()
}
