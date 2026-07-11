package com.dororong.rodi.core.domain.usecase.entry

import com.dororong.rodi.core.domain.model.entry.EntryProgress
import com.dororong.rodi.core.domain.repository.EntryRepository
import javax.inject.Inject

class SaveEntryProgressUseCase @Inject constructor(
    private val entryRepository: EntryRepository,
) {
    suspend operator fun invoke(progress: EntryProgress) = entryRepository.saveProgress(progress)
}
