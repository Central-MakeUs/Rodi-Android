package com.dororong.rodi.core.data.source.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.dororong.rodi.core.domain.model.driving.LiveUpdateSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.liveUpdateDataStore by preferencesDataStore(name = "live_update")

@Singleton
class LiveUpdatePreferences @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    val settings: Flow<LiveUpdateSettings> = context.liveUpdateDataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences ->
            LiveUpdateSettings(isEnabled = preferences[KEY_ENABLED] ?: true)
        }
        .distinctUntilChanged()

    suspend fun setEnabled(enabled: Boolean) {
        context.liveUpdateDataStore.edit { preferences -> preferences[KEY_ENABLED] = enabled }
    }

    private companion object {
        val KEY_ENABLED = booleanPreferencesKey("live_update_enabled")
    }
}
