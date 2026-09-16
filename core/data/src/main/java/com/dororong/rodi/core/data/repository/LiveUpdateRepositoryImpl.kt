package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.datastore.LiveUpdatePreferences
import com.dororong.rodi.core.domain.model.driving.LiveUpdateSettings
import com.dororong.rodi.core.domain.repository.LiveUpdateRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class LiveUpdateRepositoryImpl @Inject constructor(
    private val preferences: LiveUpdatePreferences,
) : LiveUpdateRepository {
    override val settings: Flow<LiveUpdateSettings> = preferences.settings

    override suspend fun setEnabled(enabled: Boolean) = preferences.setEnabled(enabled)
}
