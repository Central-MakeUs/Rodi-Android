package com.dororong.rodi.feature.home.map

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MapMarkerGeometryTest {
    @Test
    fun `cluster body and tail share one seam coordinate`() {
        val geometry = clusterSilhouetteGeometry(bodyBottom = 42f)

        assertEquals(geometry.bodyBottom, geometry.tailTop)
    }

    @Test
    fun `parking morph uses the same duration forward and backward`() {
        assertEquals(260L, parkingMarkerMorphDuration(0f, 1f))
        assertEquals(260L, parkingMarkerMorphDuration(1f, 0f))
    }

    @Test
    fun `parking morph resumes with proportional remaining duration`() {
        assertEquals(65L, parkingMarkerMorphDuration(0.75f, 1f))
        assertEquals(195L, parkingMarkerMorphDuration(0.75f, 0f))
    }
}
