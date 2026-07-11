package com.dororong.rodi.core.domain.usecase

import com.dororong.rodi.core.domain.EntryProgress
import com.dororong.rodi.core.domain.EntryRepository
import javax.inject.Inject

class SaveEntryProgressUseCase @Inject constructor(
    private val entryRepository: EntryRepository,
) {
    suspend operator fun invoke(progress: EntryProgress) = entryRepository.saveProgress(progress)
}
