package com.dororong.rodi.core.data.repository

import android.content.Context
import com.dororong.rodi.core.data.cache.PracticeRecordPresenceCache
import com.dororong.rodi.core.data.source.local.datastore.AuthTokenDataStore
import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.local.security.AuthTokens
import com.dororong.rodi.core.data.test.assertThrowsSuspend
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.repository.EntryRepository
import com.dororong.rodi.core.domain.repository.OnboardingRepository
import com.dororong.rodi.core.domain.repository.PracticeSessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthSessionCoordinatorTest {
    private val practiceSessionRepository = mockk<PracticeSessionRepository>(relaxed = true)
    private val onboardingRepository = mockk<OnboardingRepository>(relaxed = true)
    private val entryRepository = mockk<EntryRepository>(relaxed = true)
    private val dataStore = mockk<AuthTokenDataStore>()
    private val tokenStore = realTokenStore()
    private val coordinator = AuthSessionCoordinator(
        tokenStore,
        practiceSessionRepository,
        PracticeRecordPresenceCache(),
        onboardingRepository,
        entryRepository,
    )

    @Test
    fun `로그아웃은 세션의 로컬 저장소를 모두 지운 뒤에 알린다`() = runTest {
        val cleared = mutableListOf<String>()
        coEvery { onboardingRepository.clear() } coAnswers { cleared += "onboarding" }
        coEvery { entryRepository.clear() } coAnswers { cleared += "entry" }
        val clearedWhenPublished = async { coordinator.observeSignOut().first().let { cleared.toList() } }
        runCurrent()

        val localCleanupSucceeded = coordinator.signOut(tokenStore.getTokens()!!)

        assertTrue(localCleanupSucceeded)
        assertEquals(listOf("onboarding", "entry"), clearedWhenPublished.await())
        assertNull(tokenStore.getTokens())
    }

    @Test
    fun `새 로그인은 진행 중인 로그아웃을 기다리고 자기 온보딩 정보는 지우지 않는다`() = runTest {
        val onboardingCleanup = CompletableDeferred<Unit>()
        val cleanupStarted = CompletableDeferred<Unit>()
        coEvery { onboardingRepository.clear() } coAnswers {
            cleanupStarted.complete(Unit)
            onboardingCleanup.await()
        }
        val signOut = async { coordinator.signOut(tokenStore.getTokens()!!) }
        cleanupStarted.await()

        val login = launch { coordinator.start("access-b", "refresh-b", false) }
        runCurrent()
        assertNull(tokenStore.getTokens())

        onboardingCleanup.complete(Unit)
        signOut.await()
        login.join()

        assertEquals("access-b", tokenStore.getTokens()?.accessToken)
        coVerify(exactly = 1) { onboardingRepository.clear() }
        coVerify(exactly = 1) { entryRepository.clear() }
    }

    @Test
    fun `호출자가 취소돼도 로그아웃의 로컬 정리를 끝까지 수행한다`() = runTest {
        val onboardingCleanup = CompletableDeferred<Unit>()
        val cleanupStarted = CompletableDeferred<Unit>()
        coEvery { onboardingRepository.clear() } coAnswers {
            cleanupStarted.complete(Unit)
            onboardingCleanup.await()
        }
        val published = async { coordinator.observeSignOut().first() }
        var returnedNormally = false
        val signOut = launch {
            coordinator.signOut(tokenStore.getTokens()!!)
            returnedNormally = true
        }
        cleanupStarted.await()

        signOut.cancel()
        onboardingCleanup.complete(Unit)
        signOut.join()

        assertFalse(returnedNormally)
        published.await()
        coVerify(exactly = 1) { entryRepository.clear() }
    }

    @Test
    fun `호출자가 취소돼도 로그인의 로컬 반영을 끝까지 수행한다`() = runTest {
        coordinator.expire(tokenStore.getTokens()!!)
        val practiceCleanup = CompletableDeferred<Unit>()
        val cleanupStarted = CompletableDeferred<Unit>()
        coEvery { practiceSessionRepository.clear() } coAnswers {
            cleanupStarted.complete(Unit)
            practiceCleanup.await()
        }
        var returnedNormally = false
        val login = launch {
            coordinator.start("access-b", "refresh-b", false)
            returnedNormally = true
        }
        cleanupStarted.await()

        login.cancel()
        practiceCleanup.complete(Unit)
        login.join()

        assertFalse(returnedNormally)
        assertEquals("access-b", tokenStore.getTokens()?.accessToken)
        assertFalse(coordinator.observeExpiration().first())
    }

    @Test
    fun `토큰 영구 삭제가 실패해도 로그아웃은 메모리 세션을 끝낸다`() = runTest {
        coEvery { dataStore.clear(any()) } returns false
        val published = async { coordinator.observeSignOut().first() }
        runCurrent()

        val localCleanupSucceeded = coordinator.signOut(tokenStore.getTokens()!!)

        assertFalse(localCleanupSucceeded)
        assertNull(tokenStore.getTokens())
        published.await()
    }

    @Test
    fun `부가 저장소 정리가 실패해도 로그아웃을 계속하고 실패를 보고한다`() = runTest {
        coEvery { practiceSessionRepository.clear() } throws IllegalStateException("datastore unavailable")

        val localCleanupSucceeded = coordinator.signOut(tokenStore.getTokens()!!)

        assertFalse(localCleanupSucceeded)
        assertNull(tokenStore.getTokens())
        coVerify(exactly = 1) { onboardingRepository.clear() }
        coVerify(exactly = 1) { entryRepository.clear() }
    }

    @Test
    fun `이전 세션의 로그아웃은 교체된 세션을 지우거나 알리지 않는다`() = runTest {
        val oldSession = tokenStore.getTokens()!!
        coordinator.start("access-b", "refresh-b", false)
        var published = false
        val observer = launch { coordinator.observeSignOut().collect { published = true } }
        runCurrent()

        assertThrowsSuspend<AuthException.NotAuthenticated> { coordinator.signOut(oldSession) }
        runCurrent()
        observer.cancel()

        assertEquals("access-b", tokenStore.getTokens()?.accessToken)
        assertFalse(published)
        coVerify(exactly = 0) { onboardingRepository.clear() }
        coVerify(exactly = 0) { entryRepository.clear() }
    }

    @Test
    fun `세션 만료는 다음 로그인을 위해 기기의 온보딩 정보를 유지한다`() = runTest {
        assertTrue(coordinator.expire(tokenStore.getTokens()!!))

        assertNull(tokenStore.getTokens())
        assertTrue(coordinator.observeExpiration().first())
        coVerify(exactly = 0) { onboardingRepository.clear() }
        coVerify(exactly = 0) { entryRepository.clear() }
    }

    private fun realTokenStore(): AuthTokenStore {
        val context = mockk<Context>()
        every { context.deleteSharedPreferences(any()) } returns true
        coEvery { dataStore.read() } returns AuthTokens("access-a", "refresh-a", "kakao")
        coEvery { dataStore.save(any()) } returns true
        coEvery { dataStore.clear(any()) } returns true
        return spyk(AuthTokenStore(context, dataStore)).also {
            coEvery { it.clearCourseRegistrationData() } returns Unit
        }
    }
}
