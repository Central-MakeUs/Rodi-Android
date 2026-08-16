package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.datastore.DrivingSessionPreferences
import com.dororong.rodi.core.domain.model.driving.DrivingSession
import com.dororong.rodi.core.domain.repository.DrivingSessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class DrivingSessionRepositoryImpl @Inject constructor(
    private val preferences: DrivingSessionPreferences,
) : DrivingSessionRepository {
    override val session: Flow<DrivingSession?> = preferences.session

    override suspend fun start(session: DrivingSession) = preferences.start(session)

    override suspend fun updateProgress(
        sessionId: String,
        traveledDistanceMeters: Double,
    ) = preferences.updateProgress(sessionId, traveledDistanceMeters)

    override suspend fun markArrived(
        sessionId: String,
        arrivedAtEpochMillis: Long,
        traveledDistanceMeters: Double,
    ): Boolean = preferences.markArrived(sessionId, arrivedAtEpochMillis, traveledDistanceMeters)

    override suspend fun acknowledgeArrival(sessionId: String) =
        preferences.acknowledgeArrival(sessionId)

    override suspend fun clear(sessionId: String) = preferences.clear(sessionId)
}
