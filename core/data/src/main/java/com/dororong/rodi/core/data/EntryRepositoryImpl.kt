package com.dororong.rodi.core.data

import android.content.Context
import com.dororong.rodi.core.domain.EntryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class EntryRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
) : EntryRepository {
    private val prefs = EntryPreferences(context)

    override val isCompleted: Flow<Boolean?> = prefs.isCompleted

    override suspend fun setCompleted() = prefs.setCompleted()
}
