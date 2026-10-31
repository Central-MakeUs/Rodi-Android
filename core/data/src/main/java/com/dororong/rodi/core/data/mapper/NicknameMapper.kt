package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.model.member.MemberUpdateResponse
import com.dororong.rodi.core.data.source.remote.model.member.NicknameAvailabilityResponse
import com.dororong.rodi.core.domain.model.member.NicknameAvailability
import com.dororong.rodi.core.domain.model.member.NicknameRejectedException
import com.dororong.rodi.core.domain.model.member.NicknameRejection
import com.dororong.rodi.core.domain.model.member.NicknameUpdate
import java.time.LocalDateTime

fun NicknameAvailabilityResponse.toDomain(): NicknameAvailability {
    val rejection = reason?.toNicknameRejection()
    check(available || rejection != null) { "Unknown nickname rejection" }
    return NicknameAvailability(
        nickname = nickname,
        isAvailable = available,
        rejection = rejection,
        nicknameChangeableAt = parseNicknameChangeableAt(nicknameChangeableAt, rejection),
    )
}

fun MemberUpdateResponse.toDomain() = NicknameUpdate(
    nickname = requireNotNull(nickname?.takeIf(String::isNotBlank)) { "Missing updated nickname" },
    nicknameChangeableAt = parseNicknameChangeableAt(nicknameChangeableAt),
)

internal fun String.toNicknameRejection(): NicknameRejection? = when (this) {
    "MEMBER_400_2" -> NicknameRejection.LENGTH
    "MEMBER_400_3" -> NicknameRejection.CHARACTERS
    "MEMBER_400_4" -> NicknameRejection.PROHIBITED_WORD
    "MEMBER_400_5" -> NicknameRejection.CHANGE_RESTRICTED
    "MEMBER_409_3" -> NicknameRejection.DUPLICATE
    else -> null
}

internal fun NicknameRejection.toNicknameRejectedException(nicknameChangeableAt: String?) =
    NicknameRejectedException(this, parseNicknameChangeableAt(nicknameChangeableAt, this))

/** 서버가 시간대를 명시하지 않은 wall-clock을 내려주므로 기기 시간대나 KST를 적용하지 않는다. */
private fun parseNicknameChangeableAt(value: String?, rejection: NicknameRejection? = null): LocalDateTime? {
    val parsed = value?.let(LocalDateTime::parse)
    require(rejection != NicknameRejection.CHANGE_RESTRICTED || parsed != null) {
        "Missing nickname change restriction timestamp"
    }
    return parsed
}
