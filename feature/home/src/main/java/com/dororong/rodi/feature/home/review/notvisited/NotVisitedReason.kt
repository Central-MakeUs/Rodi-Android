package com.dororong.rodi.feature.home.review.notvisited

enum class NotVisitedReason(val label: String) {
    TRAFFIC_CHECK("실시간 교통정보를 보려고 했어요"),
    TOO_FAR("생각보다 멀었어요"),
    LOOKS_HARD("길이 어려워 보여요"),
    SCHEDULE("일정이 맞지 않았어요"),
    OTHER("기타"),
}
