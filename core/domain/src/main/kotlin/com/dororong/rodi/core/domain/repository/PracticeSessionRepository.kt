package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.practice.ActivePracticeSession

interface PracticeSessionRepository {
    suspend fun read(): ActivePracticeSession?

    suspend fun save(session: ActivePracticeSession)

    suspend fun clear()
}
