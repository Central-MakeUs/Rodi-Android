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
    /** 실제 GPS 측정 없이 경로만 연 세션인지. */
    val isMeasured: Boolean = true,
)
