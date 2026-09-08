package com.dororong.rodi.core.common

private const val DEFAULT_USER_MESSAGE = "요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요."

interface UserMessageProvider {
    val userMessage: String
}

fun Throwable?.userMessage(fallback: String = DEFAULT_USER_MESSAGE): String =
    (this as? UserMessageProvider)?.userMessage?.takeIf(String::isNotBlank) ?: fallback
