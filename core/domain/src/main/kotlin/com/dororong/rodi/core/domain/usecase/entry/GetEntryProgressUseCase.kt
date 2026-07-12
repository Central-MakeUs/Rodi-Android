package com.dororong.rodi.core.domain.usecase.entry

import com.dororong.rodi.core.domain.model.entry.EntryProgress
import com.dororong.rodi.core.domain.repository.EntryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEntryProgressUseCase @Inject constructor(
    private val entryRepository: EntryRepository,
) {
    operator fun invoke(): Flow<EntryProgress> = entryRepository.progress
}
