package com.dororong.rodi.core.domain.model.practice

import com.dororong.rodi.core.domain.model.place.PlaceType
import java.time.Instant

data class ActivePracticeSession(
    val placeId: Long,
    val placeName: String,
    val placeType: PlaceType,
    val startedAt: Instant,
    val practiceId: Long? = null,
    val isCompleted: Boolean = false,
    val isArrivalConfirmed: Boolean = false,
    /** 알림 권한을 거부해 실제 GPS 측정 없이 경로만 열었는지 — false면 10분 재진입 휴리스틱 대상. */
    val isMeasured: Boolean = true,
)
