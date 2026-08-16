package com.dororong.rodi.core.data.source.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dororong.rodi.core.domain.model.driving.DrivingNavigation
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.drivingNavigationDataStore by preferencesDataStore(name = "driving_navigation")
private val KEY_PLACE_ID = longPreferencesKey("place_id")
private val KEY_LAUNCHED_AT = longPreferencesKey("launched_at_epoch_millis")
private val KEY_MEASUREMENT_STARTED = booleanPreferencesKey("measurement_started")

@Singleton
class DrivingNavigationPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    val navigation: Flow<DrivingNavigation?> = context.drivingNavigationDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map(Preferences::toDrivingNavigation)
        .distinctUntilChanged()

    suspend fun save(navigation: DrivingNavigation) {
        context.drivingNavigationDataStore.edit { preferences ->
            preferences[KEY_PLACE_ID] = navigation.placeId
            preferences[KEY_LAUNCHED_AT] = navigation.launchedAtEpochMillis
            preferences[KEY_MEASUREMENT_STARTED] = navigation.measurementStarted
        }
    }

    suspend fun clear() {
        context.drivingNavigationDataStore.edit { it.clear() }
    }

}

private fun Preferences.toDrivingNavigation(): DrivingNavigation? {
    val placeId = this[KEY_PLACE_ID] ?: return null
    val launchedAt = this[KEY_LAUNCHED_AT] ?: return null
    val measurementStarted = this[KEY_MEASUREMENT_STARTED] ?: return null
    return DrivingNavigation(placeId, launchedAt, measurementStarted)
}
