package com.dororong.rodi.core.domain.usecase

import com.dororong.rodi.core.domain.EntryRepository
import javax.inject.Inject

class GrantGuestAccessUseCase @Inject constructor(
    private val entryRepository: EntryRepository,
) {
    suspend operator fun invoke() = entryRepository.grantGuestAccess()
}
