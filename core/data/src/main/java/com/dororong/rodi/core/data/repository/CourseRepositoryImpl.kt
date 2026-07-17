package com.dororong.rodi.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import com.dororong.rodi.core.data.source.remote.directions.KakaoDirectionsClient
import com.dororong.rodi.core.data.mapper.toDomain
import com.dororong.rodi.core.data.source.local.sample.SampleCourses
import com.dororong.rodi.core.domain.model.course.Course
import com.dororong.rodi.core.domain.repository.CourseRepository
import com.dororong.rodi.core.domain.model.course.RouteResult
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.savedCourseDataStore by preferencesDataStore(name = "saved_courses")
private val savedCourseIdSetKey = stringSetPreferencesKey("saved_course_id_set")
private const val legacySavedCourseIdsKey = "saved_course_ids"

class CourseRepositoryImpl @Inject constructor(
    private val directionsClient: KakaoDirectionsClient,
    @param:ApplicationContext private val context: Context,
) : CourseRepository {
    override fun getCourses(): List<Course> = SampleCourses.RODI_COURSES

    override suspend fun getRoute(course: Course): RouteResult {
        return directionsClient.getRoute(course).toDomain()
    }

    override fun observeSavedCourseIds(): Flow<Set<Int>> = context.savedCourseDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences.savedCourseIds()
                .mapNotNull(String::toIntOrNull)
                .toSet()
        }

    override suspend fun toggleSavedCourse(courseId: Int) {
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
