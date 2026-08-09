package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.practice.PracticeSession

interface PracticeSessionRepository {
    suspend fun get(): PracticeSession?
    suspend fun save(session: PracticeSession)
    suspend fun clear()
}
