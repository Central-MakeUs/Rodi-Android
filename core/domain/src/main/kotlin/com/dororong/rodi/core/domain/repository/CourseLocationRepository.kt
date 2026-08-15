package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.course.CourseLocationSearchResult
import com.dororong.rodi.core.domain.model.course.CourseLocationSuggestion
import com.dororong.rodi.core.domain.model.course.GeoPoint
import kotlinx.coroutines.flow.Flow

interface CourseLocationRepository {
    fun observeRecent(): Flow<List<CourseLocationSuggestion>>
    suspend fun search(keyword: String): CourseLocationSearchResult
    suspend fun resolveSelection(suggestion: CourseLocationSuggestion): CourseLocationSuggestion?
    suspend fun reverseGeocode(point: GeoPoint): CourseLocationSuggestion?
    suspend fun saveRecent(suggestion: CourseLocationSuggestion)
    suspend fun deleteRecent(id: String)
    suspend fun clearRecent()
}
