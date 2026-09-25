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

/** 이 장소의 GPS 측정이 아직 끝나지 않았는지. 운전 추적은 이 조건이 깨지면 함께 끝난다. */
fun ActivePracticeSession?.isMeasuringAt(placeId: Long): Boolean =
    this != null && isMeasured && !isCompleted && this.placeId == placeId
