package com.dororong.rodi.core.data.source.local.preferences

import android.content.Context
import androidx.core.content.edit
import com.dororong.rodi.core.domain.model.practice.PracticeSession
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PracticeSessionPreference @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun get(): PracticeSession? {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!preferences.contains(KEY_PLACE_ID) || !preferences.contains(KEY_PLACE_NAME) || !preferences.contains(KEY_STARTED_AT)) return null
        return PracticeSession(
            placeId = preferences.getLong(KEY_PLACE_ID, 0L),
            placeName = preferences.getString(KEY_PLACE_NAME, null) ?: return null,
            startedAt = Instant.ofEpochMilli(preferences.getLong(KEY_STARTED_AT, 0L)),
        )
    }

    fun save(session: PracticeSession) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putLong(KEY_PLACE_ID, session.placeId)
            putString(KEY_PLACE_NAME, session.placeName)
            putLong(KEY_STARTED_AT, session.startedAt.toEpochMilli())
        }
    }

    fun clear() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { clear() }

    private companion object {
        const val PREFS_NAME = "rodi_practice"
        const val KEY_PLACE_ID = "place_id"
        const val KEY_PLACE_NAME = "place_name"
        const val KEY_STARTED_AT = "started_at"
    }
}
