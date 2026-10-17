package com.dororong.rodi.core.domain.model.member

data class NicknameAvailability(
    val nickname: String,
    val isAvailable: Boolean,
    val rejection: NicknameRejection?,
)

enum class NicknameRejection {
    LENGTH,
    CHARACTERS,
    PROHIBITED_WORD,
    DUPLICATE,
}

class NicknameRejectedException(val rejection: NicknameRejection) : Exception()
