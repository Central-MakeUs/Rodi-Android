package com.dororong.rodi.core.common

/**
 * 예외를 사용자에게 보여줄 문구로 바꾼다.
 *
 * `message`를 그대로 쓸 수 있는 건 데이터 계층이 서버 응답과 로컬 실패를 이미 갈라놓기 때문이다.
 * 서버가 준 문구만 `message`에 남고, 역직렬화·매핑 실패처럼 개발자용 텍스트를 달고 오는 예외는
 * `AuthErrorMapper`에서 일반 문구로 치환된다. 그 분리가 깨지면 여기로 원문이 새어나간다.
 */
fun Throwable.userMessage(): String = message?.takeIf(String::isNotBlank)
    ?: "요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요."
