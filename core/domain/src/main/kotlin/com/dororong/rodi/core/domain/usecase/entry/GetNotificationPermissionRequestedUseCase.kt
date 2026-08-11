package com.dororong.rodi.core.domain.usecase.entry

import com.dororong.rodi.core.domain.repository.EntryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetNotificationPermissionRequestedUseCase @Inject constructor(
    private val entryRepository: EntryRepository,
) {
    operator fun invoke(): Flow<Boolean> = entryRepository.hasRequestedNotificationPermission
}
