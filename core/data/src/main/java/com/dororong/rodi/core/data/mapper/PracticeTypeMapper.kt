package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.domain.model.place.PracticeType

fun PracticeType.toApiValue(): String = when (this) {
    PracticeType.U_TURN -> "U_TURN"
    PracticeType.LEFT_RIGHT_TURN -> "LEFT_RIGHT_TURN"
    PracticeType.PARKING -> "PARKING"
    PracticeType.LANE_CHANGE -> "LANE_CHANGE"
    PracticeType.INTERSECTION -> "INTERSECTION"
    PracticeType.ROUNDABOUT -> "ROUNDABOUT"
    PracticeType.UNPROTECTED_LEFT_TURN -> "UNPROTECTED_LEFT_TURN"
    PracticeType.HIGHWAY_ENTRY -> "HIGHWAY_ENTRY"
    PracticeType.CORNERING -> "CORNERING"
    PracticeType.NARROW_ROAD -> "NARROW_ROAD"
    PracticeType.MULTILANE -> "MULTILANE"
    PracticeType.MERGING -> "MERGING"
    PracticeType.STRAIGHT -> "STRAIGHT"
}
