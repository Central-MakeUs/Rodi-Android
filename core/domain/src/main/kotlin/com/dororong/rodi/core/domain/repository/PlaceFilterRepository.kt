package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.place.PracticeType

interface PlaceFilterRepository {
    suspend fun getPracticeTypes(): Set<PracticeType>
    suspend fun setPracticeTypes(practiceTypes: Set<PracticeType>)
}
