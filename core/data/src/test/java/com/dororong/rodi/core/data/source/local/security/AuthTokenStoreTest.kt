package com.dororong.rodi.core.data.source.local.security

import android.content.Context
import com.dororong.rodi.core.data.source.local.datastore.AuthTokenDataStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AuthTokenStoreTest {
    @Test
    fun `clear removes tokens while preserving the recent provider`() = runTest {
        val context = mockk<Context>()
        val dataStore = mockk<AuthTokenDataStore>()
        val tokens = AuthTokens("access", "refresh", KAKAO_PROVIDER)
        every { context.deleteSharedPreferences(any()) } returns true
        coEvery { dataStore.read() } returns tokens
        coEvery { dataStore.clear(KAKAO_PROVIDER) } returns true
        val store = AuthTokenStore(context, dataStore)
        store.getTokens()

        val cleared = store.clear()

        assertTrue(cleared)
        coVerify { dataStore.clear(KAKAO_PROVIDER) }
    }

    @Test
    fun `rotation preserves login identity even when both tokens change`() = runTest {
        val store = storeWithTokens()
        val before = store.getTokens()!!

        val result = store.rotate(before, "rotated-access", "rotated-refresh", true)
        val after = store.getTokens()!!

        assertEquals(AuthTokenMutationResult.APPLIED, result)
        assertEquals(before.sessionId, after.sessionId)
        assertEquals("rotated-access", after.accessToken)
        assertEquals("rotated-refresh", after.refreshToken)
        assertTrue(after.isCourseTutorialCompleted)
    }

    @Test
    fun `a new login replaces identity even if token values are identical`() = runTest {
        val store = storeWithTokens()
        val before = store.getTokens()!!

        store.save(before.accessToken, before.refreshToken, before.provider)
        val replacement = store.getTokens()!!
        val result = store.rotate(before, "stale-access", "stale-refresh", false)

        assertNotEquals(before.sessionId, replacement.sessionId)
        assertEquals(AuthTokenMutationResult.STALE, result)
        assertEquals(replacement, store.getTokens())
    }

    @Test
    fun `a cleared session cannot be restored by rotation`() = runTest {
        val store = storeWithTokens()
        val before = store.getTokens()!!
        store.clear()

        val result = store.rotate(before, "stale-access", "stale-refresh", false)

        assertEquals(AuthTokenMutationResult.STALE, result)
        assertNull(store.getTokens())
    }

    @Test
    fun `conditional clear leaves a replacement session intact`() = runTest {
        val store = storeWithTokens()
        val before = store.getTokens()!!
        store.save("replacement", "replacement-refresh")

        val result = store.clearSession(before.sessionId)

        assertEquals(AuthTokenMutationResult.STALE, result)
        assertEquals("replacement", store.getTokens()?.accessToken)
    }

    private fun storeWithTokens(): AuthTokenStore {
        val context = mockk<Context>()
        val dataStore = mockk<AuthTokenDataStore>()
        every { context.deleteSharedPreferences(any()) } returns true
        coEvery { dataStore.read() } returns AuthTokens("access", "refresh", KAKAO_PROVIDER)
        coEvery { dataStore.save(any()) } returns true
        coEvery { dataStore.clear(any()) } returns true
        return AuthTokenStore(context, dataStore)
    }
}
