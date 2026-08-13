package com.dororong.rodi.core.data.source.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.domain.repository.PracticePromptDismissalRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private val Context.practicePromptDataStore by preferencesDataStore(name = "practice_prompt")

@Singleton
class PracticePromptDismissalStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val tokenStore: AuthTokenStore,
) : PracticePromptDismissalRepository {
    override suspend fun readDismissedPracticeIds(): Set<Long> = withContext(Dispatchers.IO) {
        val key = userKey() ?: return@withContext emptySet()
        try {
            context.practicePromptDataStore.data.first()[key]
                .orEmpty()
                .mapNotNull(String::toLongOrNull)
                .toSet()
        } catch (_: IOException) {
            emptySet()
        }
    }

    override suspend fun dismiss(practiceId: Long) {
        updateIds { it + practiceId.toString() }
    }

    override suspend fun restore(practiceId: Long) {
        updateIds { it - practiceId.toString() }
    }

    private suspend fun updateIds(transform: (Set<String>) -> Set<String>) = withContext(Dispatchers.IO) {
        val key = userKey() ?: return@withContext
        try {
            context.practicePromptDataStore.edit { preferences ->
                preferences[key] = transform(preferences[key].orEmpty())
            }
        } catch (_: IOException) {
        }
    }

    private suspend fun userKey() = tokenStore.getTokens()
        ?.refreshToken
        ?.takeIf(String::isNotBlank)
        ?.let { refreshToken -> stringSetPreferencesKey("dismissed_${refreshToken.sha256()}") }

    private fun String.sha256(): String = MessageDigest
        .getInstance("SHA-256")
        .digest(toByteArray())
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}
