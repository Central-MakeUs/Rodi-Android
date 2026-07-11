package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.source.remote.directions.KakaoDirectionsClient
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.course.RouteResult

fun KakaoDirectionsClient.RouteResult.toDomain(): RouteResult = RouteResult(
    points = points.map { GeoPoint(it.latitude, it.longitude) },
    isRealRoute = isRealRoute,
    totalDistanceMeters = totalDistanceMeters,
    snappedPoints = snappedPoints.map { GeoPoint(it.latitude, it.longitude) },
)
