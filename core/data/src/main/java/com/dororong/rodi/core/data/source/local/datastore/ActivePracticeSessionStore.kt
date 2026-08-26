package com.dororong.rodi.core.data.source.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.practice.ActivePracticeSession
import com.dororong.rodi.core.domain.repository.PracticeSessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.activePracticeSessionDataStore by preferencesDataStore(name = "active_practice_session")

@Singleton
class ActivePracticeSessionStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val tokenStore: AuthTokenStore,
) : PracticeSessionRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val activeSessionKey = stringPreferencesKey("active")

    override suspend fun read(): ActivePracticeSession? = withContext(Dispatchers.IO) {
        if (tokenStore.getTokens() == null) return@withContext null
        try {
            context.activePracticeSessionDataStore.data.first()[activeSessionKey]
                ?.let { encoded -> json.decodeFromString<StoredActivePracticeSession>(encoded) }
                ?.takeUnless(StoredActivePracticeSession::isCompleted)
                ?.toDomain()
        } catch (_: IOException) {
            null
        } catch (_: SerializationException) {
            null
        }
    }

    override suspend fun save(session: ActivePracticeSession) {
        withContext(Dispatchers.IO) {
            checkNotNull(tokenStore.getTokens()) { "로그인이 필요한 연습 측정입니다." }
            context.activePracticeSessionDataStore.edit { preferences ->
                preferences[activeSessionKey] = json.encodeToString(session.toStored())
            }
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            context.activePracticeSessionDataStore.edit { preferences ->
                preferences.remove(activeSessionKey)
            }
        }
    }

    private fun ActivePracticeSession.toStored() = StoredActivePracticeSession(
        placeId = placeId,
        placeName = placeName,
        placeType = placeType.name,
        startedAtEpochMillis = startedAt.toEpochMilli(),
        practiceId = practiceId,
        isCompleted = isCompleted,
        isArrivalConfirmed = isArrivalConfirmed,
    )

    private fun StoredActivePracticeSession.toDomain(): ActivePracticeSession? {
        val type = PlaceType.entries.firstOrNull { it.name == placeType } ?: return null
        return ActivePracticeSession(
            placeId = placeId,
            placeName = placeName,
            placeType = type,
            startedAt = Instant.ofEpochMilli(startedAtEpochMillis),
            practiceId = practiceId,
            isCompleted = isCompleted,
            isArrivalConfirmed = isArrivalConfirmed,
        )
    }

}

@Serializable
private data class StoredActivePracticeSession(
    val placeId: Long = 0L,
    val placeName: String = "",
    val placeType: String = "COURSE",
    val startedAtEpochMillis: Long = 0L,
    val practiceId: Long? = null,
    val isCompleted: Boolean = false,
    val isArrivalConfirmed: Boolean = false,
)
