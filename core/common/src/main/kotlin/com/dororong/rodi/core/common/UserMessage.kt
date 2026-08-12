package com.dororong.rodi.core.common

interface UserMessageProvider {
    val userMessage: String
}

/**
 * 승인된 사용자용 문구를 반환하고, 그렇지 않으면 안전한 기본 문구를 반환한다.
 */
fun Throwable.userMessage(): String = (this as? UserMessageProvider)?.userMessage
    ?.takeIf(String::isNotBlank)
    ?: "요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요."
