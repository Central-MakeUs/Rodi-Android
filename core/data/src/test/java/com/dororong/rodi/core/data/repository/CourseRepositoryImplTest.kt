package com.dororong.rodi.core.data.repository

import android.content.Context
import com.dororong.rodi.core.data.source.local.sample.SampleCourses
import com.dororong.rodi.core.data.source.remote.directions.KakaoDirectionsClient
import com.kakao.vectormap.LatLng
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CourseRepositoryImplTest {
    @Test
    fun `maps injected directions result to domain route`() = runTest {
        val directionsClient = mockk<KakaoDirectionsClient>()
        val course = SampleCourses.RODI_COURSES.first()
        coEvery { directionsClient.getRoute(course) } returns KakaoDirectionsClient.RouteResult(
            points = listOf(LatLng.from(37.1, 127.1)),
            isRealRoute = true,
            totalDistanceMeters = 1_200,
            snappedPoints = listOf(LatLng.from(37.2, 127.2)),
        )

        val result = CourseRepositoryImpl(
            directionsClient = directionsClient,
            context = mockk<Context>(relaxed = true),
        ).getRoute(course)

        assertTrue(result.isRealRoute)
        assertEquals(1_200, result.totalDistanceMeters)
        assertEquals(37.1, result.points.single().lat)
        assertEquals(127.2, result.snappedPoints.single().lng)
    }
}
