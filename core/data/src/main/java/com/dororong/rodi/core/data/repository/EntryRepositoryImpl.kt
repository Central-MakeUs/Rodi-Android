package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.datastore.EntryPreferences
import com.dororong.rodi.core.domain.model.entry.EntryProgress
import com.dororong.rodi.core.domain.repository.EntryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class EntryRepositoryImpl @Inject constructor(
    private val prefs: EntryPreferences,
) : EntryRepository {
    override val isCompleted: Flow<Boolean?> = prefs.isCompleted
    override val hasGuestAccess: Flow<Boolean> = prefs.hasGuestAccess
    override val progress: Flow<EntryProgress> = prefs.progress

    override suspend fun setCompleted() = prefs.setCompleted()
    override suspend fun grantGuestAccess() = prefs.grantGuestAccess()
    override suspend fun saveProgress(progress: EntryProgress) = prefs.saveProgress(progress)
}
