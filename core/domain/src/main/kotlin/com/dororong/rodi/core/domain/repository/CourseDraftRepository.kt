package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.course.CourseDraft
import kotlinx.coroutines.flow.Flow

interface CourseDraftRepository {
    fun observe(): Flow<CourseDraft?>
    suspend fun save(draft: CourseDraft)
    suspend fun clear()
}
