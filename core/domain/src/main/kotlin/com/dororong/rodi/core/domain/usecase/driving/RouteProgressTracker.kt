package com.dororong.rodi.core.domain.usecase.driving

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.driving.DrivingLocationSample
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

data class RouteProgress(
    val isInCourseScope: Boolean,
    val recognizedDistanceMeters: Double,
    val requiredDistanceMeters: Int,
    val progressPercent: Int,
    val isComplete: Boolean,
)

class RouteProgressTracker(
    route: List<GeoPoint>,
    private val requiredDistanceMeters: Int,
    private val scopeRadiusMeters: Double = 200.0,
    private val maxAccuracyMeters: Float = 50f,
    private val forwardSearchMeters: Double = 300.0,
    private val backwardToleranceMeters: Double = 50.0,
    private val maxPlausibleSpeedMetersPerSecond: Double = 40.0,
    private val maxRecoveryGapMillis: Long = 300_000L,
) {
    // route.distinct()를 쓰면 안 된다 — 왕복 코스는 출발점이 목록 끝에 다시 나오는데,
    // distinct는 리스트 전체에서 중복을 지우기 때문에 그 왕복 구간 자체가 사라진다.
    // 여기서는 "바로 이전 지점과 같은 경우"만 지운다(연속 중복 좌표 정리 목적).
    private val vertices = buildVertices(route.dropConsecutiveDuplicates())
    private val totalRouteLengthMeters = vertices.lastOrNull()?.cumulativeDistance ?: 0.0

    private var pointerArcLengthMeters = 0.0
    private var completed = false
    private var lastSampleElapsedRealtimeMillis: Long? = null

    init {
        require(requiredDistanceMeters >= 0)
        require(scopeRadiusMeters > 0)
        require(maxAccuracyMeters > 0)
        require(forwardSearchMeters > 0)
        require(backwardToleranceMeters >= 0)
        require(maxPlausibleSpeedMetersPerSecond > 0)
        require(maxRecoveryGapMillis >= 0)
    }

    fun add(sample: DrivingLocationSample): RouteProgress {
        if (completed || vertices.size < 2) return progress(isInCourseScope = false)
        if (sample.accuracyMeters !in 0f..maxAccuracyMeters) return progress(isInCourseScope = false)

        val windowStart = (pointerArcLengthMeters - backwardToleranceMeters).coerceAtLeast(0.0)
        val windowEnd = (pointerArcLengthMeters + forwardSearchMeters).coerceAtMost(totalRouteLengthMeters)

        var bestArcLength: Double? = null
        var bestPerpendicularDistance = Double.MAX_VALUE
        for (index in 0 until vertices.size - 1) {
            val start = vertices[index]
            val end = vertices[index + 1]
            if (end.cumulativeDistance < windowStart || start.cumulativeDistance > windowEnd) continue
            val projection = sample.point.projectOntoSegment(start.point, end.point)
            val arcLength = start.cumulativeDistance +
                projection.fraction * (end.cumulativeDistance - start.cumulativeDistance)
            if (arcLength !in windowStart..windowEnd) continue
            if (projection.perpendicularDistanceMeters < bestPerpendicularDistance) {
                bestPerpendicularDistance = projection.perpendicularDistanceMeters
                bestArcLength = arcLength
            }
        }

        if (bestArcLength == null) {
            val previousSampleElapsedRealtimeMillis = lastSampleElapsedRealtimeMillis
            if (previousSampleElapsedRealtimeMillis != null) {
                val elapsedMillis =
                    (sample.elapsedRealtimeMillis - previousSampleElapsedRealtimeMillis).coerceAtLeast(0)
                val cappedElapsedMillis = elapsedMillis.coerceAtMost(maxRecoveryGapMillis)
                val plausibleJumpMeters =
                    (cappedElapsedMillis / 1000.0) * maxPlausibleSpeedMetersPerSecond
                val recoveryWindowEnd =
                    (pointerArcLengthMeters + plausibleJumpMeters).coerceAtMost(totalRouteLengthMeters)
                if (recoveryWindowEnd > windowEnd) {
                    for (index in 0 until vertices.size - 1) {
                        val start = vertices[index]
                        val end = vertices[index + 1]
                        if (end.cumulativeDistance < windowStart || start.cumulativeDistance > recoveryWindowEnd) {
                            continue
                        }
                        val projection = sample.point.projectOntoSegment(start.point, end.point)
                        val arcLength = start.cumulativeDistance +
                            projection.fraction * (end.cumulativeDistance - start.cumulativeDistance)
                        if (arcLength !in windowStart..recoveryWindowEnd) continue
                        if (projection.perpendicularDistanceMeters < bestPerpendicularDistance) {
                            bestPerpendicularDistance = projection.perpendicularDistanceMeters
                            bestArcLength = arcLength
                        }
                    }
                }
            }
        }

        lastSampleElapsedRealtimeMillis = sample.elapsedRealtimeMillis
        val isInCourseScope = bestArcLength != null && bestPerpendicularDistance <= scopeRadiusMeters
        if (isInCourseScope) {
            pointerArcLengthMeters = maxOf(pointerArcLengthMeters, requireNotNull(bestArcLength))
        }
        return progress(isInCourseScope).also { if (it.isComplete) completed = true }
    }

    private fun progress(isInCourseScope: Boolean): RouteProgress {
        val percent = if (requiredDistanceMeters > 0) {
            (pointerArcLengthMeters / requiredDistanceMeters * 100).roundToInt().coerceIn(0, 100)
        } else {
            0
        }
        return RouteProgress(
            isInCourseScope = isInCourseScope,
            recognizedDistanceMeters = pointerArcLengthMeters,
            requiredDistanceMeters = requiredDistanceMeters,
            progressPercent = percent,
            isComplete = requiredDistanceMeters > 0 && pointerArcLengthMeters >= requiredDistanceMeters,
        )
    }

    private companion object {
        fun buildVertices(route: List<GeoPoint>): List<Vertex> {
            if (route.isEmpty()) return emptyList()
            var cumulative = 0.0
            val result = ArrayList<Vertex>(route.size)
            result.add(Vertex(route[0], 0.0))
            for (index in 1 until route.size) {
                cumulative += route[index - 1].distanceTo(route[index])
                result.add(Vertex(route[index], cumulative))
            }
            return result
        }
    }

    private data class Vertex(val point: GeoPoint, val cumulativeDistance: Double)
}

private fun List<GeoPoint>.dropConsecutiveDuplicates(): List<GeoPoint> {
    if (isEmpty()) return this
    val result = ArrayList<GeoPoint>(size)
    result.add(first())
    for (index in 1 until size) {
        if (this[index] != result.last()) result.add(this[index])
    }
    return result
}

/** 방문 인증 필요거리 = 코스 전체 거리의 40%, 최대 5km. */
fun requiredCertifiedDistanceMeters(
    courseDistanceMeters: Int,
    recognitionRatio: Double = 0.4,
    maximumDistanceMeters: Int = 5_000,
): Int {
    require(courseDistanceMeters >= 0)
    require(recognitionRatio in 0.0..1.0)
    require(maximumDistanceMeters >= 0)
    return min((courseDistanceMeters * recognitionRatio).roundToInt(), maximumDistanceMeters)
}

private data class SegmentProjection(val perpendicularDistanceMeters: Double, val fraction: Double)

private fun GeoPoint.projectOntoSegment(start: GeoPoint, end: GeoPoint): SegmentProjection {
    val earthRadiusMeters = 6_371_000.0
    val referenceLatitude = Math.toRadians((start.lat + end.lat + lat) / 3.0)
    val longitudeScale = cos(referenceLatitude)

    fun x(point: GeoPoint) = Math.toRadians(point.lng) * earthRadiusMeters * longitudeScale
    fun y(point: GeoPoint) = Math.toRadians(point.lat) * earthRadiusMeters

    val startX = x(start)
    val startY = y(start)
    val endX = x(end)
    val endY = y(end)
    val pointX = x(this)
    val pointY = y(this)
    val deltaX = endX - startX
    val deltaY = endY - startY
    val lengthSquared = deltaX * deltaX + deltaY * deltaY
    val fraction = if (lengthSquared == 0.0) {
        0.0
    } else {
        ((pointX - startX) * deltaX + (pointY - startY) * deltaY) / lengthSquared
    }.coerceIn(0.0, 1.0)
    val distance = hypot(
        pointX - (startX + fraction * deltaX),
        pointY - (startY + fraction * deltaY),
    )
    return SegmentProjection(perpendicularDistanceMeters = distance, fraction = fraction)
}
