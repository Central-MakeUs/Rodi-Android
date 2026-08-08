package com.dororong.rodi.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith

private const val ROUTE_TRANSITION_MILLIS = 120

/**
 * navigation3 `NavDisplay`의 기본 전환은 700ms 페이드라, 이전 화면이 그동안 반투명으로 남는다.
 * 지도 위에 떠 있는 검색창처럼 그림자를 가진 요소가 잔상처럼 보여서 전환을 짧게 줄였다.
 */
internal val ROUTE_FADE: ContentTransform =
    fadeIn(tween(ROUTE_TRANSITION_MILLIS)) togetherWith fadeOut(tween(ROUTE_TRANSITION_MILLIS))
