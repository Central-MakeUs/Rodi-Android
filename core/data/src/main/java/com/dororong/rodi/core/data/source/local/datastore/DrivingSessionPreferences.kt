package com.dororong.rodi.core.data.source.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.driving.DrivingSession
import com.dororong.rodi.core.domain.model.driving.DrivingSessionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.drivingSessionDataStore by preferencesDataStore(name = "driving_session")

@Singleton
class DrivingSessionPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    val session: Flow<DrivingSession?> = context.drivingSessionDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { it.toDrivingSession() }
        .distinctUntilChanged()

    suspend fun start(session: DrivingSession) {
        context.drivingSessionDataStore.edit { it.write(session) }
    }

    suspend fun updateProgress(
        sessionId: String,
        traveledDistanceMeters: Double,
    ) {
        context.drivingSessionDataStore.edit { preferences ->
            val current = preferences.toDrivingSession()
            if (current?.id == sessionId && current.status == DrivingSessionStatus.ACTIVE) {
                preferences[KEY_TRAVELED_DISTANCE_METERS] = traveledDistanceMeters
            }
        }
    }

    suspend fun markArrived(
        sessionId: String,
        arrivedAtEpochMillis: Long,
        traveledDistanceMeters: Double,
    ): Boolean {
        var transitioned = false
        context.drivingSessionDataStore.edit { preferences ->
            val current = preferences.toDrivingSession()
            if (current?.id == sessionId && current.status == DrivingSessionStatus.ACTIVE) {
                preferences.write(
                    current.copy(
                        arrivedAtEpochMillis = arrivedAtEpochMillis,
                        traveledDistanceMeters = traveledDistanceMeters,
                        status = DrivingSessionStatus.ARRIVED,
                        isArrivalNoticePending = true,
                    ),
                )
                transitioned = true
            }
        }
        return transitioned
    }

    suspend fun acknowledgeArrival(sessionId: String) {
        context.drivingSessionDataStore.edit { preferences ->
            val current = preferences.toDrivingSession()
            if (
                current?.id == sessionId &&
                current.status == DrivingSessionStatus.ARRIVED &&
                current.isArrivalNoticePending
            ) {
                preferences[KEY_ARRIVAL_NOTICE_PENDING] = false
            }
        }
    }

    suspend fun clear(sessionId: String) {
        context.drivingSessionDataStore.edit { preferences ->
            if (preferences[KEY_ID] == sessionId) preferences.clear()
        }
    }

    private companion object {
        val KEY_ID = stringPreferencesKey("id")
        val KEY_PLACE_ID = longPreferencesKey("place_id")
        val KEY_PLACE_NAME = stringPreferencesKey("place_name")
        val KEY_DESTINATION_LAT = doublePreferencesKey("destination_lat")
        val KEY_DESTINATION_LNG = doublePreferencesKey("destination_lng")
        val KEY_PLANNED_DISTANCE_METERS = intPreferencesKey("planned_distance_meters")
        val KEY_STARTED_AT_EPOCH_MILLIS = longPreferencesKey("started_at_epoch_millis")
        val KEY_ARRIVED_AT_EPOCH_MILLIS = longPreferencesKey("arrived_at_epoch_millis")
        val KEY_TRAVELED_DISTANCE_METERS = doublePreferencesKey("traveled_distance_meters")
        val KEY_STATUS = stringPreferencesKey("status")
        val KEY_ARRIVAL_NOTICE_PENDING = booleanPreferencesKey("arrival_notice_pending")
    }

    private fun MutablePreferences.write(session: DrivingSession) {
        this[KEY_ID] = session.id
        this[KEY_PLACE_ID] = session.placeId
        this[KEY_PLACE_NAME] = session.placeName
        this[KEY_DESTINATION_LAT] = session.destination.lat
        this[KEY_DESTINATION_LNG] = session.destination.lng
        session.plannedDistanceMeters?.let { this[KEY_PLANNED_DISTANCE_METERS] = it }
            ?: remove(KEY_PLANNED_DISTANCE_METERS)
        this[KEY_STARTED_AT_EPOCH_MILLIS] = session.startedAtEpochMillis
        session.arrivedAtEpochMillis?.let { this[KEY_ARRIVED_AT_EPOCH_MILLIS] = it }
            ?: remove(KEY_ARRIVED_AT_EPOCH_MILLIS)
        this[KEY_TRAVELED_DISTANCE_METERS] = session.traveledDistanceMeters
        this[KEY_STATUS] = session.status.name
        this[KEY_ARRIVAL_NOTICE_PENDING] = session.isArrivalNoticePending
    }

    private fun Preferences.toDrivingSession(): DrivingSession? {
        val id = this[KEY_ID] ?: return null
        val placeId = this[KEY_PLACE_ID] ?: return null
        val placeName = this[KEY_PLACE_NAME] ?: return null
        val destinationLat = this[KEY_DESTINATION_LAT] ?: return null
        val destinationLng = this[KEY_DESTINATION_LNG] ?: return null
        val startedAt = this[KEY_STARTED_AT_EPOCH_MILLIS] ?: return null
        val status = this[KEY_STATUS]
            ?.let { value -> DrivingSessionStatus.entries.firstOrNull { it.name == value } }
            ?: return null
        return DrivingSession(
            id = id,
            placeId = placeId,
            placeName = placeName,
            destination = GeoPoint(destinationLat, destinationLng),
            plannedDistanceMeters = this[KEY_PLANNED_DISTANCE_METERS],
            startedAtEpochMillis = startedAt,
            arrivedAtEpochMillis = this[KEY_ARRIVED_AT_EPOCH_MILLIS],
            traveledDistanceMeters = this[KEY_TRAVELED_DISTANCE_METERS] ?: 0.0,
            status = status,
            isArrivalNoticePending = this[KEY_ARRIVAL_NOTICE_PENDING] ?: false,
        )
    }
}
