package com.dororong.rodi.core.domain

import kotlinx.coroutines.flow.Flow

interface EntryRepository {
    val isCompleted: Flow<Boolean?>
    suspend fun setCompleted()
}
