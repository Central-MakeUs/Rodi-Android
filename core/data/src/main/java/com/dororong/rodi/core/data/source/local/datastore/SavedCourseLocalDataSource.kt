package com.dororong.rodi.core.data.source.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.savedCourseDataStore by preferencesDataStore(name = "saved_courses")
private val savedCourseIdSetKey = stringSetPreferencesKey("saved_course_id_set")
private const val legacySavedCourseIdsKey = "saved_course_ids"

@Singleton
class SavedCourseLocalDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun observeSavedCourseIds(): Flow<Set<Int>> = context.savedCourseDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences.savedCourseIds()
                .mapNotNull(String::toIntOrNull)
                .toSet()
        }

    suspend fun toggleSavedCourse(courseId: Int) {
        context.savedCourseDataStore.edit { preferences ->
            val ids = preferences.savedCourseIds().toMutableSet()
            if (!ids.add(courseId.toString())) ids.remove(courseId.toString())
            preferences[savedCourseIdSetKey] = ids
        }
    }
}

internal fun Preferences.savedCourseIds(): Set<String> {
    val values = asMap()
    val savedIds = values.entries
        .firstOrNull { it.key == savedCourseIdSetKey }
        ?.value as? Set<*>
    if (savedIds != null) return savedIds.filterIsInstance<String>().toSet()

    return when (val legacyValue = values.entries.firstOrNull { it.key.name == legacySavedCourseIdsKey }?.value) {
        is Set<*> -> legacyValue.filterIsInstance<String>().toSet()
        is String -> legacyValue.split(Regex("[^0-9]+"))
            .filter(String::isNotBlank)
            .toSet()
        else -> emptySet()
    }
}
