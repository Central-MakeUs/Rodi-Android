package com.dororong.rodi.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
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
            preferences[KEY_SAVED_COURSE_IDS]
                .orEmpty()
                .mapNotNull(String::toIntOrNull)
                .toSet()
        }

    override suspend fun toggleSavedCourse(courseId: Int) {
        context.savedCourseDataStore.edit { preferences ->
            val ids = preferences[KEY_SAVED_COURSE_IDS].orEmpty().toMutableSet()
            if (!ids.add(courseId.toString())) ids.remove(courseId.toString())
            preferences[KEY_SAVED_COURSE_IDS] = ids
        }
    }

    private companion object {
        val KEY_SAVED_COURSE_IDS = stringSetPreferencesKey("saved_course_ids")
    }
}
