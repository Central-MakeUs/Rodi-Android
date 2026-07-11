package com.dororong.rodi.core.domain.usecase

import com.dororong.rodi.core.domain.EntryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGuestAccessUseCase @Inject constructor(
    private val entryRepository: EntryRepository,
) {
    operator fun invoke(): Flow<Boolean> = entryRepository.hasGuestAccess
}
