package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.preferences.PracticeSessionPreference
import com.dororong.rodi.core.domain.model.practice.PracticeSession
import com.dororong.rodi.core.domain.repository.PracticeSessionRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PracticeSessionRepositoryImpl @Inject constructor(
    private val preference: PracticeSessionPreference,
) : PracticeSessionRepository {
    override suspend fun get(): PracticeSession? = withContext(Dispatchers.IO) { preference.get() }
    override suspend fun save(session: PracticeSession) = withContext(Dispatchers.IO) { preference.save(session) }
    override suspend fun clear() = withContext(Dispatchers.IO) { preference.clear() }
}
