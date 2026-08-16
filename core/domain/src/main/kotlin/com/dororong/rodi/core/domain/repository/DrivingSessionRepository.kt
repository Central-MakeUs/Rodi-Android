package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.driving.DrivingSession
import kotlinx.coroutines.flow.Flow

interface DrivingSessionRepository {
    val session: Flow<DrivingSession?>

    suspend fun start(session: DrivingSession)

    suspend fun updateProgress(
        sessionId: String,
        traveledDistanceMeters: Double,
    )

    suspend fun markArrived(
        sessionId: String,
        arrivedAtEpochMillis: Long,
        traveledDistanceMeters: Double,
    ): Boolean

    suspend fun acknowledgeArrival(sessionId: String)

    suspend fun clear(sessionId: String)
}
