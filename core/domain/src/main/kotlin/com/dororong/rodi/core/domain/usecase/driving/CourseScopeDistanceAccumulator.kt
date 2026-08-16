package com.dororong.rodi.core.domain.usecase.driving

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.driving.DrivingLocationSample
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

data class CourseProgress(
    val isInCourseScope: Boolean,
    val recognizedDistanceMeters: Double,
    val requiredDistanceMeters: Int,
    val progressPercent: Int,
    val isComplete: Boolean,
)

/**
 * 코스 폴리라인 주변에서 실제로 이동한 거리만 인정한다.
 * 위치 표본이 코스 밖으로 나가면 다음 코스 안 표본까지의 이동은 인정하지 않는다.
 */
class CourseScopeDistanceAccumulator(
    route: List<GeoPoint>,
    private val requiredDistanceMeters: Int,
    private val scopeRadiusMeters: Double = 150.0,
    private val maxAccuracyMeters: Float = 50f,
    private val minMovementMeters: Double = 3.0,
    private val maxMovementMeters: Double = 200.0,
) {
    private val route = route.distinct()
    private var previousSample: DrivingLocationSample? = null
    private var previousInCourseScope = false
    private var completed = false

    var recognizedDistanceMeters: Double = 0.0
        private set

    init {
        require(requiredDistanceMeters >= 0)
        require(scopeRadiusMeters > 0)
        require(maxAccuracyMeters > 0)
        require(minMovementMeters >= 0)
        require(maxMovementMeters >= minMovementMeters)
    }

    fun add(sample: DrivingLocationSample): CourseProgress {
        if (completed) return progress(isInCourseScope = previousInCourseScope)
        if (sample.accuracyMeters !in 0f..maxAccuracyMeters) {
            previousSample = null
            previousInCourseScope = false
            return progress(isInCourseScope = false)
        }

        val isInCourseScope = route.isNotEmpty() && route.distanceTo(sample.point) <= scopeRadiusMeters
        val previous = previousSample
        if (
            previous != null &&
            previousInCourseScope &&
            isInCourseScope &&
            sample.elapsedRealtimeMillis > previous.elapsedRealtimeMillis
        ) {
            val movement = previous.point.distanceTo(sample.point)
            when {
                movement in minMovementMeters..maxMovementMeters -> {
                    recognizedDistanceMeters += movement
                }
                movement > maxMovementMeters -> {
                    previousSample = null
                    previousInCourseScope = false
                    return progress(isInCourseScope)
                }
            }
        }

        previousSample = sample
        previousInCourseScope = isInCourseScope
        return progress(isInCourseScope).also {
            if (it.isComplete) completed = true
        }
    }

    private fun progress(isInCourseScope: Boolean): CourseProgress {
        val percentage = if (requiredDistanceMeters > 0) {
            (recognizedDistanceMeters / requiredDistanceMeters * 100).roundToInt()
                .coerceIn(0, 100)
        } else {
            0
        }
        return CourseProgress(
            isInCourseScope = isInCourseScope,
            recognizedDistanceMeters = recognizedDistanceMeters,
            requiredDistanceMeters = requiredDistanceMeters,
            progressPercent = percentage,
            isComplete = requiredDistanceMeters > 0 &&
                recognizedDistanceMeters >= requiredDistanceMeters,
        )
    }
}

fun requiredCertifiedDistanceMeters(
    courseDistanceMeters: Int,
    recognitionRatio: Double = 0.4,
    maximumDistanceMeters: Int = 5_000,
): Int {
    require(courseDistanceMeters >= 0)
    require(recognitionRatio in 0.0..1.0)
    require(maximumDistanceMeters >= 0)
    return min(
        (courseDistanceMeters * recognitionRatio).roundToInt(),
        maximumDistanceMeters,
    )
}

private fun List<GeoPoint>.distanceTo(point: GeoPoint): Double = when {
    isEmpty() -> Double.POSITIVE_INFINITY
    size == 1 -> first().distanceTo(point)
    else -> zipWithNext().minOf { (start, end) -> point.distanceToSegment(start, end) }
}

private fun GeoPoint.distanceToSegment(start: GeoPoint, end: GeoPoint): Double {
    val earthRadiusMeters = 6_371_000.0
    val referenceLatitude = Math.toRadians((start.lat + end.lat + lat) / 3.0)
    val longitudeScale = cos(referenceLatitude)

    fun x(point: GeoPoint): Double = Math.toRadians(point.lng) * earthRadiusMeters * longitudeScale
    fun y(point: GeoPoint): Double = Math.toRadians(point.lat) * earthRadiusMeters

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
    return hypot(
        pointX - (startX + fraction * deltaX),
        pointY - (startY + fraction * deltaY),
    )
}
