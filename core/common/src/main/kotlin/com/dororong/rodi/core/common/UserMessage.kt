package com.dororong.rodi.core.common

private const val DEFAULT_USER_MESSAGE = "요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요."

interface UserMessageProvider {
    val userMessage: String
}

/**
 * 승인된 사용자용 문구를 반환하고, 그렇지 않으면 호출부가 정한 맥락 문구를 반환한다.
 * 예외의 `message`는 절대 쓰지 않는다 — 직렬화 실패 등의 원문이 화면에 노출된다.
 */
fun Throwable?.userMessage(fallback: String = DEFAULT_USER_MESSAGE): String =
    (this as? UserMessageProvider)?.userMessage?.takeIf(String::isNotBlank) ?: fallback
