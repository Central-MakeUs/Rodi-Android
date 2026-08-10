package com.dororong.rodi.core.domain.model.practice

import java.time.Instant

data class PracticeSession(
    val placeId: Long,
    val placeName: String,
    val startedAt: Instant,
)
