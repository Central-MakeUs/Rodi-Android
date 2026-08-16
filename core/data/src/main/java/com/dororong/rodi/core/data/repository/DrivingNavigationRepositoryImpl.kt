package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.datastore.DrivingNavigationPreferences
import com.dororong.rodi.core.domain.model.driving.DrivingNavigation
import com.dororong.rodi.core.domain.repository.DrivingNavigationRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class DrivingNavigationRepositoryImpl @Inject constructor(
    private val preferences: DrivingNavigationPreferences,
) : DrivingNavigationRepository {
    override val navigation: Flow<DrivingNavigation?> = preferences.navigation

    override suspend fun save(navigation: DrivingNavigation) = preferences.save(navigation)

    override suspend fun clear() = preferences.clear()
}
