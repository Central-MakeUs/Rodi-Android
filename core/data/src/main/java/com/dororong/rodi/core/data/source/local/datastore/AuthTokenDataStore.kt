package com.dororong.rodi.core.data.source.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dororong.rodi.core.data.source.local.security.AuthTokens
import com.dororong.rodi.core.data.source.local.security.EncryptedPayload
import com.dororong.rodi.core.data.source.local.security.TokenEncryption
import com.dororong.rodi.core.data.source.local.security.TokenEncryptionException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authTokenDataStore by preferencesDataStore(name = "auth_tokens")

@Singleton
class AuthTokenDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val tokenEncryption: TokenEncryption,
    private val json: Json,
) {
    suspend fun read(): AuthTokens? {
        return try {
            val encodedTokens = context.authTokenDataStore.data.first()[KEY_TOKENS] ?: return null
            val tokens = json.decodeFromString<EncryptedAuthTokens>(encodedTokens)
            AuthTokens(
                accessToken = tokenEncryption.decrypt(tokens.accessToken, ACCESS_TOKEN_AAD),
                refreshToken = tokenEncryption.decrypt(tokens.refreshToken, REFRESH_TOKEN_AAD),
                provider = tokenEncryption.decrypt(tokens.provider, PROVIDER_AAD),
            )
        } catch (_: TokenEncryptionException) {
            clear()
            null
        } catch (_: SerializationException) {
            clear()
            null
        } catch (_: IOException) {
            clear()
            null
        }
    }

    suspend fun readRecentProvider(): String? =
        try {
            context.authTokenDataStore.data.first()[KEY_RECENT_PROVIDER]
        } catch (_: IOException) {
            null
        }

    suspend fun save(tokens: AuthTokens): Boolean = try {
        val encryptedTokens = EncryptedAuthTokens(
            accessToken = tokenEncryption.encrypt(tokens.accessToken, ACCESS_TOKEN_AAD),
            refreshToken = tokenEncryption.encrypt(tokens.refreshToken, REFRESH_TOKEN_AAD),
            provider = tokenEncryption.encrypt(tokens.provider, PROVIDER_AAD),
        )
        context.authTokenDataStore.edit { preferences ->
            preferences[KEY_TOKENS] = json.encodeToString(encryptedTokens)
            preferences[KEY_RECENT_PROVIDER] = tokens.provider
        }
        true
    } catch (_: TokenEncryptionException) {
        false
    } catch (_: IOException) {
        false
    }

    suspend fun clear(recentProvider: String? = null): Boolean = try {
        context.authTokenDataStore.edit { preferences ->
            preferences.remove(KEY_TOKENS)
            if (recentProvider != null) preferences[KEY_RECENT_PROVIDER] = recentProvider
        }
        true
    } catch (_: IOException) {
        false
    }

    @Serializable
    private data class EncryptedAuthTokens(
        val accessToken: EncryptedPayload,
        val refreshToken: EncryptedPayload,
        val provider: EncryptedPayload,
    )

    private companion object {
        val KEY_TOKENS = stringPreferencesKey("tokens")
        val KEY_RECENT_PROVIDER = stringPreferencesKey("recent_provider")
        const val ACCESS_TOKEN_AAD = "rodi:auth:access:v1"
        const val REFRESH_TOKEN_AAD = "rodi:auth:refresh:v1"
        const val PROVIDER_AAD = "rodi:auth:provider:v1"
    }
}
