package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.entry.EntryProgress
import com.dororong.rodi.core.domain.model.entry.EntryMode
import kotlinx.coroutines.flow.Flow

interface EntryRepository {
    val isCompleted: Flow<Boolean?>
    val hasGuestAccess: Flow<Boolean>
    val hasRequestedLocationPermission: Flow<Boolean>
    val progress: Flow<EntryProgress>
    suspend fun setCompleted()
    suspend fun start(mode: EntryMode)
    suspend fun markLocationPermissionRequested()
    suspend fun grantGuestAccess()
    suspend fun clearGuestAccess()
    suspend fun clear()
    suspend fun saveProgress(progress: EntryProgress)
}
