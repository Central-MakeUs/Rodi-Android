package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.datastore.CourseDraftDataStore
import com.dororong.rodi.core.domain.model.course.CourseDraft
import com.dororong.rodi.core.domain.repository.CourseDraftRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class CourseDraftRepositoryImpl @Inject constructor(
    private val dataStore: CourseDraftDataStore,
) : CourseDraftRepository {
    override fun observe(): Flow<CourseDraft?> = dataStore.observe()

    override suspend fun save(draft: CourseDraft) {
        if (draft.isMeaningful) dataStore.save(draft) else dataStore.clear()
    }

    override suspend fun clear() = dataStore.clear()
}
