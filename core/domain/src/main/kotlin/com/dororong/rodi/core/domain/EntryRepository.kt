package com.dororong.rodi.core.domain

import kotlinx.coroutines.flow.Flow

interface EntryRepository {
    val isCompleted: Flow<Boolean?>
    val hasGuestAccess: Flow<Boolean>
    val progress: Flow<EntryProgress>
    suspend fun setCompleted()
    suspend fun grantGuestAccess()
    suspend fun saveProgress(progress: EntryProgress)
}
