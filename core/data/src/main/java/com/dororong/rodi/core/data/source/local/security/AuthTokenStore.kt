package com.dororong.rodi.core.data.source.local.security

import android.content.Context
import com.dororong.rodi.core.data.source.local.datastore.AuthTokenDataStore
import com.dororong.rodi.core.data.source.local.datastore.CourseDraftDataStore
import com.dororong.rodi.core.data.source.local.datastore.CourseSearchHistoryDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
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
            readLocked()
        }
    }

    suspend fun getRecentProvider(): String? = withContext(Dispatchers.IO) {
        dataStore.readRecentProvider()
    }

    suspend fun save(
        accessToken: String,
        refreshToken: String,
        provider: String = KAKAO_PROVIDER,
        isCourseTutorialCompleted: Boolean = false,
    ): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val tokens = AuthTokens(accessToken, refreshToken, provider, isCourseTutorialCompleted)
            saveLocked(tokens)
        }
    }

    suspend fun rotate(
        expected: AuthTokens,
        accessToken: String,
        refreshToken: String,
        isCourseTutorialCompleted: Boolean,
    ): AuthTokenMutationResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = readLocked()
            if (current?.sessionId != expected.sessionId || current.refreshToken != expected.refreshToken) {
                return@withLock AuthTokenMutationResult.STALE
            }
            val updated = current.copy(
                accessToken = accessToken,
                refreshToken = refreshToken,
                isCourseTutorialCompleted = isCourseTutorialCompleted,
            )
            if (saveLocked(updated)) AuthTokenMutationResult.APPLIED else AuthTokenMutationResult.FAILED
        }
    }

    suspend fun clearSession(sessionId: String): AuthTokenMutationResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (readLocked()?.sessionId != sessionId) return@withLock AuthTokenMutationResult.STALE
            if (clearLocked()) AuthTokenMutationResult.APPLIED else AuthTokenMutationResult.FAILED
        }
    }

    suspend fun markCourseTutorialCompleted(): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = readLocked() ?: return@withLock false
            val updated = current.copy(isCourseTutorialCompleted = true)
            val saved = dataStore.save(updated)
            if (saved) cachedTokens = updated
            saved
        }
    }

    suspend fun clearCourseRegistrationData() = withContext(Dispatchers.IO) {
        val draftResult = runCatching { CourseDraftDataStore.clearForContext(context) }
        val historyResult = runCatching { CourseSearchHistoryDataStore.clearForContext(context) }
        (draftResult.exceptionOrNull() as? CancellationException)?.let { throw it }
        (historyResult.exceptionOrNull() as? CancellationException)?.let { throw it }
        draftResult.getOrThrow()
        historyResult.getOrThrow()
    }

    suspend fun clear(): Boolean = withContext(Dispatchers.IO) { mutex.withLock { clearLocked() } }

    private suspend fun readLocked(): AuthTokens? {
        if (!cacheInitialized) {
            removeLegacyStore()
            cachedTokens = dataStore.read()
            cacheInitialized = true
        }
        return cachedTokens
    }

    private suspend fun saveLocked(tokens: AuthTokens): Boolean {
        removeLegacyStore()
        val saved = dataStore.save(tokens)
        if (saved) {
            cachedTokens = tokens
            cacheInitialized = true
        } else {
            clearLocked()
        }
        return saved
    }

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
