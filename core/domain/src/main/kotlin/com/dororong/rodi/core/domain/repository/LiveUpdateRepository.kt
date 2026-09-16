package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.driving.LiveUpdateSettings
import kotlinx.coroutines.flow.Flow

interface LiveUpdateRepository {
    val settings: Flow<LiveUpdateSettings>

    suspend fun setEnabled(enabled: Boolean)
}
