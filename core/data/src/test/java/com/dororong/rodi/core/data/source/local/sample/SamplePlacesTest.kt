package com.dororong.rodi.core.data.source.local.sample

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SamplePlacesTest {
    @Test
    fun `sample places use negative IDs and keep both course and parking markers`() {
        val coordinates = SamplePlaces.coordinates()

        assertTrue(coordinates.isNotEmpty())
        assertTrue(coordinates.all { it.id < 0L })
        assertTrue(coordinates.any { it.type == PlaceType.COURSE })
        assertTrue(coordinates.any { it.type == PlaceType.PARKING })
    }

    @Test
    fun `course summary in viewport opens with its waypoints`() {
        val query = PlaceViewportQuery(
            southWest = GeoPoint(37.45, 126.85),
            northEast = GeoPoint(37.70, 127.20),
            origin = GeoPoint(37.55, 126.98),
        )

        val course = SamplePlaces.summaries(query).first { it.type == PlaceType.COURSE }
        val detail = SamplePlaces.detail(course.id)

        assertNotNull(detail)
        assertTrue(detail?.course?.waypoints.orEmpty().size >= 2)
        assertFalse(SamplePlaces.isSamplePlace(1L))
    }
}
