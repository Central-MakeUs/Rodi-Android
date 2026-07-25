package com.dororong.rodi.core.domain.usecase.auth

import com.dororong.rodi.core.domain.model.entry.EntryMode
import com.dororong.rodi.core.domain.repository.EntryRepository
import javax.inject.Inject

class GrantGuestAccessUseCase @Inject constructor(
    private val entryRepository: EntryRepository,
) {
    suspend operator fun invoke() {
        entryRepository.start(EntryMode.GUEST_BROWSE)
        entryRepository.grantGuestAccess()
    }
}
