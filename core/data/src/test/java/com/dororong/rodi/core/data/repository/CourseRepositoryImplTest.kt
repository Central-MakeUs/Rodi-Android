package com.dororong.rodi.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
    fun `reads legacy string saved course ids without a type cast`() {
        val preferences = mutablePreferencesOf(
            stringPreferencesKey("saved_course_ids") to "1, 2, invalid",
        )

        assertEquals(setOf("1", "2"), preferences.savedCourseIds())
    }

    @Test
    fun `prefers saved course id set over legacy value`() {
        val preferences = mutablePreferencesOf(
            stringPreferencesKey("saved_course_ids") to "1, 2",
            stringSetPreferencesKey("saved_course_id_set") to setOf("3", "4"),
        )

        assertEquals(setOf("3", "4"), preferences.savedCourseIds())
    }

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
