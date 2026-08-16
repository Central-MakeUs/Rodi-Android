package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.driving.DrivingNavigation
import kotlinx.coroutines.flow.Flow

interface DrivingNavigationRepository {
    val navigation: Flow<DrivingNavigation?>

    suspend fun save(navigation: DrivingNavigation)

    suspend fun clear()
}
