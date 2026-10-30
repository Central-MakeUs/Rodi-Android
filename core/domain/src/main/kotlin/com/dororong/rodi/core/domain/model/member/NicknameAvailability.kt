package com.dororong.rodi.core.domain.model.member

import java.time.LocalDateTime

data class NicknameAvailability(
    val nickname: String,
    val isAvailable: Boolean,
    val rejection: NicknameRejection?,
    val nicknameChangeableAt: LocalDateTime? = null,
)

enum class NicknameRejection {
    LENGTH,
    CHARACTERS,
    PROHIBITED_WORD,
    DUPLICATE,
    CHANGE_RESTRICTED,
}

class NicknameRejectedException(
    val rejection: NicknameRejection,
    val nicknameChangeableAt: LocalDateTime? = null,
) : Exception()

data class NicknameUpdate(
    val nickname: String,
    val nicknameChangeableAt: LocalDateTime? = null,
)
