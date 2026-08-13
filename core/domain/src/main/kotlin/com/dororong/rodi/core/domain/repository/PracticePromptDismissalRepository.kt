package com.dororong.rodi.core.domain.repository

interface PracticePromptDismissalRepository {
    suspend fun readDismissedPracticeIds(): Set<Long>

    suspend fun dismiss(practiceId: Long)

    suspend fun restore(practiceId: Long)
}
