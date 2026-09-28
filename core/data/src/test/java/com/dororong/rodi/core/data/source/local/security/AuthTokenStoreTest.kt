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
    fun `초기화는 토큰을 지우고 최근 로그인 제공자는 유지한다`() = runTest {
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
    fun `토큰이 모두 바뀌어도 교체 시 로그인 식별자는 유지한다`() = runTest {
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
    fun `토큰 값이 같아도 새 로그인은 식별자를 새로 만든다`() = runTest {
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
    fun `지워진 세션은 토큰 교체로 되살릴 수 없다`() = runTest {
        val store = storeWithTokens()
        val before = store.getTokens()!!
        store.clear()

        val result = store.rotate(before, "stale-access", "stale-refresh", false)

        assertEquals(AuthTokenMutationResult.STALE, result)
        assertNull(store.getTokens())
    }

    @Test
    fun `조건부 초기화는 교체된 세션을 건드리지 않는다`() = runTest {
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
