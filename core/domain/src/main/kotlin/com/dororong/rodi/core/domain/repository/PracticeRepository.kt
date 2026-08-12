package com.dororong.rodi.core.domain.repository

import com.dororong.rodi.core.domain.model.practice.Practice
import com.dororong.rodi.core.domain.model.practice.PracticeVisitResult
import com.dororong.rodi.core.domain.model.practice.SkipReasonForm

interface PracticeRepository {
    suspend fun register(placeId: Long): Practice
    suspend fun recordVisit(practiceId: Long, certifiedDistanceMeters: Int? = null): PracticeVisitResult
    suspend fun submitSkipReason(practiceId: Long, reason: String, detail: String?)
    suspend fun getSkipReasonForm(): SkipReasonForm
    suspend fun delete(practiceId: Long)
}
