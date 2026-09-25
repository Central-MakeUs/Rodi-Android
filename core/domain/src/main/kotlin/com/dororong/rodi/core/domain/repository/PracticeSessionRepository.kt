package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.practice.ActivePracticeSession
import kotlinx.coroutines.flow.Flow

interface PracticeSessionRepository {
    suspend fun read(): ActivePracticeSession?

    fun observe(): Flow<ActivePracticeSession?>

    suspend fun save(session: ActivePracticeSession)

    suspend fun confirmArrival(placeId: Long): Boolean

    suspend fun clear()
}
