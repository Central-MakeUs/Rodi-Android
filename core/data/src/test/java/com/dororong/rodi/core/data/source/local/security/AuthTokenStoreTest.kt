package com.dororong.rodi.core.data.source.local.security

import android.content.Context
import com.dororong.rodi.core.data.source.local.datastore.AuthTokenDataStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
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
}
