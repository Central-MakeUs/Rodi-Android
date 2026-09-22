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
    fun `sign out publishes only after every session local store is cleared`() = runTest {
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
    fun `new login waits for an in-flight sign out and keeps its own onboarding`() = runTest {
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
    fun `sign out finishes its local commit even when the caller is cancelled`() = runTest {
        val onboardingCleanup = CompletableDeferred<Unit>()
        val cleanupStarted = CompletableDeferred<Unit>()
        coEvery { onboardingRepository.clear() } coAnswers {
            cleanupStarted.complete(Unit)
            onboardingCleanup.await()
        }
        val published = async { coordinator.observeSignOut().first() }
        val signOut = launch { coordinator.signOut(tokenStore.getTokens()!!) }
        cleanupStarted.await()

        signOut.cancel()
        onboardingCleanup.complete(Unit)
        signOut.join()

        assertTrue(signOut.isCancelled)
        published.await()
        coVerify(exactly = 1) { entryRepository.clear() }
    }

    @Test
    fun `sign out ends the in-memory session when token persistence fails`() = runTest {
        coEvery { dataStore.clear(any()) } returns false
        val published = async { coordinator.observeSignOut().first() }
        runCurrent()

        val localCleanupSucceeded = coordinator.signOut(tokenStore.getTokens()!!)

        assertFalse(localCleanupSucceeded)
        assertNull(tokenStore.getTokens())
        published.await()
    }

    @Test
    fun `sign out keeps going when a secondary store fails and reports it`() = runTest {
        coEvery { practiceSessionRepository.clear() } throws IllegalStateException("datastore unavailable")

        val localCleanupSucceeded = coordinator.signOut(tokenStore.getTokens()!!)

        assertFalse(localCleanupSucceeded)
        assertNull(tokenStore.getTokens())
        coVerify(exactly = 1) { onboardingRepository.clear() }
        coVerify(exactly = 1) { entryRepository.clear() }
    }

    @Test
    fun `stale sign out neither clears nor announces the replacement session`() = runTest {
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
    fun `expiration keeps device onboarding for the next login`() = runTest {
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
