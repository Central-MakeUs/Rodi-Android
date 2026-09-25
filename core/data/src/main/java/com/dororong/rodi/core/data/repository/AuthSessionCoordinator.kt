package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.cache.PracticeRecordPresenceCache
import com.dororong.rodi.core.data.source.local.security.AuthTokenMutationResult
import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.local.security.AuthTokens
import com.dororong.rodi.core.data.source.local.security.KAKAO_PROVIDER
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.repository.EntryRepository
import com.dororong.rodi.core.domain.repository.OnboardingRepository
import com.dororong.rodi.core.domain.repository.PracticeSessionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 로그인 세션의 로컬 시작·교체·종료를 한 락 아래에서 commit한다.
 *
 * 종료는 시작한 세션이 아직 현재 세션일 때만 적용하고, 세션에 딸린 로컬 데이터를 모두 정리한 뒤에
 * 알린다. 화면이 알림을 받고 떠나며 호출 코루틴이 취소돼도 정리가 중간에 끊기지 않게 하기 위해서다.
 * 네트워크 요청은 이 락 밖에서 끝낸다.
 */
@Singleton
class AuthSessionCoordinator @Inject constructor(
    private val tokenStore: AuthTokenStore,
    private val practiceSessionRepository: PracticeSessionRepository,
    private val practiceRecordPresenceCache: PracticeRecordPresenceCache,
    private val onboardingRepository: OnboardingRepository,
    private val entryRepository: EntryRepository,
) {
    private val mutex = Mutex()
    private val expired = MutableStateFlow(false)
    private val signOuts = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun observeExpiration(): Flow<Boolean> = expired.asStateFlow()

    fun observeSignOut(): Flow<Unit> = signOuts.asSharedFlow()

    suspend fun start(accessToken: String, refreshToken: String, isCourseTutorialCompleted: Boolean) = commit {
        mutex.withLock {
            if (!tokenStore.save(accessToken, refreshToken, KAKAO_PROVIDER, isCourseTutorialCompleted)) {
                throw AuthException.Unknown("로그인 정보를 안전하게 저장하지 못했습니다.")
            }
            clearPracticeSessionSafely()
            practiceRecordPresenceCache.clear()
            expired.value = false
        }
    }

    suspend fun rotate(
        expected: AuthTokens,
        accessToken: String,
        refreshToken: String,
        isCourseTutorialCompleted: Boolean,
    ) {
        mutex.withLock {
            when (tokenStore.rotate(expected, accessToken, refreshToken, isCourseTutorialCompleted)) {
                AuthTokenMutationResult.APPLIED -> expired.value = false
                AuthTokenMutationResult.STALE -> Unit
                AuthTokenMutationResult.FAILED -> throw AuthException.Unknown("로그인 정보를 안전하게 저장하지 못했습니다.")
            }
        }
    }

    /** 서버가 [expected] 세션을 거부했다. 그 사이 다른 세션으로 바뀌었으면 아무것도 하지 않고 false. */
    suspend fun expire(expected: AuthTokens): Boolean = commit {
        mutex.withLock {
            if (tokenStore.getTokens()?.sessionId != expected.sessionId) return@withLock false
            try {
                endLocked(expected, clearsDeviceOnboarding = false)
            } finally {
                expired.value = true
            }
            true
        }
    }

    /**
     * 사용자가 [expected] 세션을 끝냈다(로그아웃·탈퇴·삭제). 서버 요청이 성공한 뒤에만 부른다.
     *
     * 다른 세션으로 이미 바뀌었으면 새 세션을 건드리지 않고 [AuthException.NotAuthenticated]를 던진다.
     * 토큰 영구 삭제가 실패해도 메모리 세션은 비워지므로 종료로 보고 알린다.
     */
    suspend fun signOut(expected: AuthTokens): Boolean = commit {
        mutex.withLock {
            if (tokenStore.getTokens()?.sessionId != expected.sessionId) {
                throw AuthException.NotAuthenticated("로그인 세션이 변경되었습니다.")
            }
            val localCleanupSucceeded = endLocked(expected, clearsDeviceOnboarding = true)
            signOuts.tryEmit(Unit)
            localCleanupSucceeded
        }
    }

    // 시작한 commit은 호출자가 취소돼도 끝까지 수행하고, 취소는 결과 대신 호출자에게 다시 던진다.
    // 같은 dispatcher의 withContext(NonCancellable)는 반환 시 취소를 확인하지 않는다.
    private suspend fun <T> commit(block: suspend () -> T): T {
        currentCoroutineContext().ensureActive()
        val result = withContext(NonCancellable) { block() }
        currentCoroutineContext().ensureActive()
        return result
    }

    private suspend fun endLocked(expected: AuthTokens, clearsDeviceOnboarding: Boolean): Boolean {
        var succeeded = when (tokenStore.clearSession(expected.sessionId)) {
            AuthTokenMutationResult.APPLIED -> true
            AuthTokenMutationResult.FAILED -> false
            AuthTokenMutationResult.STALE -> throw AuthException.NotAuthenticated("로그인 세션이 변경되었습니다.")
        }
        succeeded = attempt { practiceSessionRepository.clear() } && succeeded
        succeeded = attempt { tokenStore.clearCourseRegistrationData() } && succeeded
        practiceRecordPresenceCache.clear()
        if (clearsDeviceOnboarding) {
            succeeded = attempt { onboardingRepository.clear() } && succeeded
            succeeded = attempt { entryRepository.clear() } && succeeded
        }
        return succeeded
    }

    // 계정 전환 시 이전 계정의 연습 세션이 다음 로그인 계정에 노출되면 안 되므로 몇 번은
    // 재시도한다. 그래도 실패하면(그래도 흔치 않다) 로그인 자체는 막지 않는다 — 로컬 캐시
    // 하나 못 지웠다고 로그인이 실패하는 게 더 나쁘다.
    private suspend fun clearPracticeSessionSafely() {
        repeat(PRACTICE_SESSION_CLEAR_ATTEMPTS) {
            if (attempt { practiceSessionRepository.clear() }) return
        }
    }

    private suspend fun attempt(block: suspend () -> Unit): Boolean = try {
        block()
        true
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Throwable) {
        false
    }

    private companion object {
        const val PRACTICE_SESSION_CLEAR_ATTEMPTS = 3
    }
}
