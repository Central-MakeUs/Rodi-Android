package com.dororong.rodi.core.data.source.local.security

import android.content.Context
import com.dororong.rodi.core.data.source.local.datastore.AuthTokenDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthTokenStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dataStore: AuthTokenDataStore,
) {
    private val mutex = Mutex()

    @Volatile
    private var cachedTokens: AuthTokens? = null

    @Volatile
    private var cacheInitialized = false

    @Volatile
    private var legacyStoreRemoved = false

    suspend fun getTokens(): AuthTokens? = withContext(Dispatchers.IO) {
        if (cacheInitialized) return@withContext cachedTokens

        mutex.withLock {
            if (!cacheInitialized) {
                removeLegacyStore()
                cachedTokens = dataStore.read()
                cacheInitialized = true
            }
            cachedTokens
        }
    }

    suspend fun getRecentProvider(): String? = withContext(Dispatchers.IO) {
        dataStore.readRecentProvider()
    }

    suspend fun save(
        accessToken: String,
        refreshToken: String,
        provider: String = KAKAO_PROVIDER,
    ): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            removeLegacyStore()
            val tokens = AuthTokens(accessToken, refreshToken, provider)
            val saved = dataStore.save(tokens)
            if (saved) {
                cachedTokens = tokens
                cacheInitialized = true
            } else {
                clearLocked()
            }
            saved
        }
    }

    suspend fun clear(): Boolean = withContext(Dispatchers.IO) { mutex.withLock { clearLocked() } }

    private suspend fun clearLocked(): Boolean {
        val recentProvider = cachedTokens?.provider
        cachedTokens = null
        cacheInitialized = true
        removeLegacyStore()
        return dataStore.clear(recentProvider)
    }

    private fun removeLegacyStore() {
        if (!legacyStoreRemoved) {
            context.deleteSharedPreferences(LEGACY_PREFERENCES_NAME)
            legacyStoreRemoved = true
        }
    }

    private companion object {
        const val LEGACY_PREFERENCES_NAME = "auth_secure_prefs"
    }
}
