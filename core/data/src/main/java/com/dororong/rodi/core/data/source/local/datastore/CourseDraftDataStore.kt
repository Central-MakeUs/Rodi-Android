package com.dororong.rodi.core.data.source.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dororong.rodi.core.domain.model.course.CourseDraft
import com.dororong.rodi.core.domain.model.course.RegistrationWaypoint
import com.dororong.rodi.core.domain.model.course.RegistrationWaypointType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.courseDraftDataStore by preferencesDataStore(name = "course_registration_draft")

@Singleton
class CourseDraftDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val json: Json,
) {
    fun observe(): Flow<CourseDraft?> = context.courseDraftDataStore.data
        .map { preferences -> preferences[KEY_DRAFT]?.let(::decode) }
        .catch { error -> if (error is IOException) emit(null) else throw error }

    suspend fun save(draft: CourseDraft) {
        context.courseDraftDataStore.edit { it[KEY_DRAFT] = json.encodeToString(draft.toStored()) }
    }

    suspend fun clear() {
        context.courseDraftDataStore.edit { it.remove(KEY_DRAFT) }
    }

    private fun decode(value: String): CourseDraft? = runCatching {
        json.decodeFromString<StoredCourseDraft>(value).toDomain()
    }.getOrNull()

    companion object {
        suspend fun clearForContext(context: Context) {
            context.courseDraftDataStore.edit { it.remove(KEY_DRAFT) }
        }

        private val KEY_DRAFT = stringPreferencesKey("draft")
    }
}

@Serializable
private data class StoredCourseDraft(
    val waypoints: List<StoredRegistrationWaypoint> = emptyList(),
    val selectedPracticeTypeCodes: List<String> = emptyList(),
    val caution: String = "",
    val description: String = "",
)

@Serializable
private data class StoredRegistrationWaypoint(
    val type: String,
    val name: String,
    val address: String,
    val jibunAddress: String? = null,
    val lat: Double,
    val lng: Double,
)

private fun CourseDraft.toStored() = StoredCourseDraft(
    waypoints = waypoints.map {
        StoredRegistrationWaypoint(
            type = it.type.name,
            name = it.name,
            address = it.address,
            jibunAddress = it.jibunAddress,
            lat = it.lat,
            lng = it.lng,
        )
    },
    selectedPracticeTypeCodes = selectedPracticeTypeCodes,
    caution = caution,
    description = description,
)

private fun StoredCourseDraft.toDomain() = CourseDraft(
    waypoints = waypoints.map {
        RegistrationWaypoint(
            type = RegistrationWaypointType.valueOf(it.type),
            name = it.name,
            address = it.address,
            jibunAddress = it.jibunAddress,
            lat = it.lat,
            lng = it.lng,
        )
    },
    selectedPracticeTypeCodes = selectedPracticeTypeCodes,
    caution = caution,
    description = description,
)
