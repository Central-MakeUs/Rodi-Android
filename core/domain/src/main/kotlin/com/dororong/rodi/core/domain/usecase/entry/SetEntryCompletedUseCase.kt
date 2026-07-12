package com.dororong.rodi.core.domain.usecase.entry

import com.dororong.rodi.core.domain.repository.EntryRepository
import javax.inject.Inject

class SetEntryCompletedUseCase @Inject constructor(
    private val entryRepository: EntryRepository,
) {
    suspend operator fun invoke() = entryRepository.setCompleted()
}
