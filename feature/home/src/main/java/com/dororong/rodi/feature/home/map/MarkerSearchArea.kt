package com.dororong.rodi.feature.home.map

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery

/**
 * 지도 마커는 화면보다 한 화면씩 넓은 범위로 받는다. 현재 위치 이동처럼 앱이 스스로 다시 검색할 때
 * 화면 바로 바깥의 장소까지 사라지지 않게 하고, 조금 움직이거나 축소해도 마커가 남아 있게 한다.
 * 목록은 화면 범위 그대로 조회한다.
 */
internal fun PlaceViewportQuery.expandedForMarkers(): PlaceViewportQuery {
    val latSpan = northEast.lat - southWest.lat
    val lngSpan = northEast.lng - southWest.lng
    if (latSpan <= 0 || lngSpan <= 0) return this
    return copy(
        southWest = GeoPoint(
            lat = (southWest.lat - latSpan).coerceAtLeast(-MAX_LAT),
            lng = (southWest.lng - lngSpan).coerceAtLeast(-MAX_LNG),
        ),
        northEast = GeoPoint(
            lat = (northEast.lat + latSpan).coerceAtMost(MAX_LAT),
            lng = (northEast.lng + lngSpan).coerceAtMost(MAX_LNG),
        ),
    )
}

private const val MAX_LAT = 85.0
private const val MAX_LNG = 180.0
