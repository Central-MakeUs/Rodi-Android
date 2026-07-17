package com.dororong.rodi.core.data.source.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.place.PracticeType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.savedPlaceDataStore by preferencesDataStore(name = "saved_places_v2")
private val savedPlacesKey = stringPreferencesKey("saved_place_snapshots")

@Singleton
class SavedPlaceLocalDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val json: Json,
) {
    fun observeSavedPlaces(): Flow<List<PlaceSummary>> = context.savedPlaceDataStore.data
        .catch { error -> if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw error }
        .map { preferences -> preferences[savedPlacesKey].decodeSavedPlaces() }

    suspend fun setBookmarked(place: PlaceDetail, bookmarked: Boolean) {
        context.savedPlaceDataStore.edit { preferences ->
            val saved = preferences[savedPlacesKey].decodeSavedPlaces().associateByTo(linkedMapOf(), PlaceSummary::id)
            if (bookmarked) saved[place.id] = place.toSummary() else saved.remove(place.id)
            preferences[savedPlacesKey] = json.encodeToString(saved.values.map(PlaceSummary::toLocal))
        }
    }

    private fun String?.decodeSavedPlaces(): List<PlaceSummary> {
        if (isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<SavedPlaceDto>>(this).map(SavedPlaceDto::toDomain) }
            .getOrDefault(emptyList())
    }
}

@Serializable
private data class SavedPlaceDto(
    val id: Long,
    val type: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val practiceTypes: List<String>,
    val description: String?,
    val distanceMeters: Int?,
    val capacity: Int?,
    val openTime: String?,
)

private fun PlaceSummary.toLocal() = SavedPlaceDto(
    id, type.name, name, address, point.lat, point.lng, practiceTypes.map(PracticeType::name),
    description, distanceMeters, capacity, openTime,
)

private fun SavedPlaceDto.toDomain() = PlaceSummary(
    id = id,
    type = PlaceType.valueOf(type),
    name = name,
    address = address,
    point = GeoPoint(lat, lng),
    distanceFromMeMeters = 0,
    practiceTypes = practiceTypes.mapNotNull { value -> PracticeType.entries.firstOrNull { it.name == value } },
    description = description,
    distanceMeters = distanceMeters,
    capacity = capacity,
    openTime = openTime,
)

private fun PlaceDetail.toSummary() = PlaceSummary(
    id = id,
    type = type,
    name = name,
    address = address,
    point = point,
    distanceFromMeMeters = 0,
    practiceTypes = practiceTypes,
    description = course?.description,
    distanceMeters = course?.distanceMeters,
    capacity = parking?.capacity,
    openTime = parking?.operatingHours?.weekday?.substringBefore("-")?.trim(),
)
