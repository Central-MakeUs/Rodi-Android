package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.domain.model.member.NicknameRejection

internal fun String.toNicknameRejection(): NicknameRejection? = when (this) {
    "MEMBER_400_2" -> NicknameRejection.LENGTH
    "MEMBER_400_3" -> NicknameRejection.CHARACTERS
    "MEMBER_400_4" -> NicknameRejection.PROHIBITED_WORD
    "MEMBER_409_3" -> NicknameRejection.DUPLICATE
    else -> null
}
