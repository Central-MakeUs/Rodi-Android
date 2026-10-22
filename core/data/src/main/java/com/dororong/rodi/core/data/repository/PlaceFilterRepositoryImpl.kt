package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.datastore.PlaceFilterPreferences
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.repository.PlaceFilterRepository
import javax.inject.Inject

class PlaceFilterRepositoryImpl @Inject constructor(
    private val preferences: PlaceFilterPreferences,
) : PlaceFilterRepository {
    override suspend fun getPracticeTypes(): Set<PracticeType> = preferences.getPracticeTypes()

    override suspend fun setPracticeTypes(practiceTypes: Set<PracticeType>) =
        preferences.setPracticeTypes(practiceTypes)
}
