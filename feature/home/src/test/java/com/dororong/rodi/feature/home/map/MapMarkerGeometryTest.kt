package com.dororong.rodi.feature.home.map

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MapMarkerGeometryTest {
    @Test
    fun `클러스터 몸통과 꼬리는 같은 이음 좌표를 쓴다`() {
        val geometry = clusterSilhouetteGeometry(bodyBottom = 42f)

        assertEquals(geometry.bodyBottom, geometry.tailTop)
    }

    @Test
    fun `현재 위치 마커는 원의 중심에 지도 좌표를 맞춘다`() {
        assertEquals(18f / 28f, currentLocationMarkerAnchorY())
    }

}
