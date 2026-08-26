package com.dororong.rodi.core.data.mock

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.CoursePlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceCoordinate
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import com.dororong.rodi.core.domain.model.place.PlaceWaypoint
import com.dororong.rodi.core.domain.model.place.PlaceWaypointType
import com.dororong.rodi.core.domain.model.place.PracticeType

/**
 * 서버에 없는, 실기기 실측 전용 테스트 코스. 디버그 빌드에서만 PlaceRepositoryImpl이 붙여
 * 서버 응답과 함께 반환한다 — 릴리스 빌드 바이너리에도 포함되지만 `BuildConfig.DEBUG` 분기로만
 * 실행된다.
 *
 * 실제 서버 id와 겹치지 않도록 큰 양수를 쓴다(음수는 PlaceCacheDao가 레거시 샘플로 간주해
 * 매 조회마다 지워버린다).
 */
object LocalTestPlaces {
    const val ID = 999_999_001L

    private val point = GeoPoint(lat = 36.1094539836781, lng = 128.42421591281894)

    fun containedIn(query: PlaceViewportQuery): Boolean =
        point.lat in query.southWest.lat..query.northEast.lat &&
            point.lng in query.southWest.lng..query.northEast.lng

    fun coordinate() = PlaceCoordinate(
        id = ID,
        type = PlaceType.COURSE,
        name = "테스트용",
        address = "경북 구미시 인동중앙로12길 15-8",
        point = point,
    )

    fun summary() = PlaceSummary(
        id = ID,
        type = PlaceType.COURSE,
        name = "테스트용",
        address = "경북 구미시 인동중앙로12길 15-8",
        point = point,
        distanceFromMeMeters = null,
        practiceTypes = listOf(PracticeType.STRAIGHT),
        description = "테스트~~~~~~~",
        distanceMeters = 700,
        capacity = null,
        openTime = null,
    )

    fun detail() = PlaceDetail(
        id = ID,
        type = PlaceType.COURSE,
        name = "테스트용",
        address = "경북 구미시 인동중앙로12길 15-8",
        point = point,
        practiceTypes = listOf(PracticeType.STRAIGHT),
        bookmarkCount = 0,
        isBookmarked = false,
        course = CoursePlaceDetail(
            description = "테스트~~~~~~~",
            cautions = emptyList(),
            distanceMeters = 700,
            waypoints = listOf(
                PlaceWaypoint(
                    type = PlaceWaypointType.START,
                    sequence = 1,
                    point = GeoPoint(36.1094539836781, 128.42421591281894),
                    name = "금오산",
                ),
                PlaceWaypoint(
                    type = PlaceWaypointType.VIA,
                    sequence = 2,
                    point = GeoPoint(36.10957966592116, 128.42333614826205),
                    name = "36.1096, 128.4233",
                ),
                PlaceWaypoint(
                    type = PlaceWaypointType.VIA,
                    sequence = 3,
                    point = GeoPoint(36.108938250988786, 128.42133522033694),
                    name = "36.1089, 128.4213",
                ),
                PlaceWaypoint(
                    type = PlaceWaypointType.DESTINATION,
                    sequence = 4,
                    point = GeoPoint(36.1083791755929, 128.4176176786423),
                    name = "36.1105, 128.4258",
                ),
            ),
        ),
        parking = null,
    )
}
