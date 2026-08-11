package com.dororong.rodi.core.data.mapper

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * 서버 시각 문자열을 [Instant]로 바꾼다.
 *
 * 서버는 OpenAPI에 `format: date-time`으로 선언해두고 실제로는 오프셋 없는 값
 * (`2026-08-10T10:47:33.996642`)을 내려보낼 때가 있다. `Instant.parse`는 오프셋이 없으면
 * 파싱하지 못해 예외를 던지고, 그 예외가 목록 전체를 날려버렸다.
 *
 * 오프셋이 없는 값은 서비스 기준 시간대([SERVER_ZONE])의 벽시계 시각으로 해석한다.
 * 표시용 포맷터가 기기 시간대를 쓰므로, 국내 사용자 기준으로는 서버가 보낸 날짜가 그대로 보인다.
 */
internal fun parseServerTimestamp(value: String): Instant =
    runCatching { Instant.parse(value) }
        .recoverCatching { OffsetDateTime.parse(value).toInstant() }
        .recoverCatching { LocalDateTime.parse(value).atZone(SERVER_ZONE).toInstant() }
        .getOrElse { throw IllegalArgumentException("Invalid timestamp: $value", it) }

/**
 * 오프셋 없는 서버 시각을 해석할 기준 시간대.
 *
 * 서버가 오프셋을 붙여 내려주면 이 값은 쓰이지 않는다. 백엔드에 오프셋 포함 응답을 요청해둔 상태다.
 */
private val SERVER_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
