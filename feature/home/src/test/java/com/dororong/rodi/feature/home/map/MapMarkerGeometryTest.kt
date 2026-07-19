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
    fun `current location marker anchors map coordinates at the circle center`() {
        assertEquals(18f / 28f, currentLocationMarkerAnchorY())
    }
}
