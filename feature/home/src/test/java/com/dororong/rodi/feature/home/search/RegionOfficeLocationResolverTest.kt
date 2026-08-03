package com.dororong.rodi.feature.home.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class RegionOfficeLocationResolverTest {
    @Test
    fun `normalizes metropolitan region names before resolving`() {
        assertEquals("서울 중구", RegionOfficeLocationResolver.find("서울특별시   중구")?.regionKey)
    }

    @Test
    fun `resolves every supplied regional name`() {
        assertNotNull(RegionOfficeLocationResolver.find("서울 강남구"))
        assertNotNull(RegionOfficeLocationResolver.find("인천 검단구"))
        assertNotNull(RegionOfficeLocationResolver.find("전남광주통합특별시 신안군"))
        assertNotNull(RegionOfficeLocationResolver.find("제주특별자치도 서귀포시"))
    }

    @Test
    fun `uses a distinct viewport point for each municipality`() {
        val jongno = RegionOfficeLocationResolver.find("서울 종로구")!!.point
        val gangnam = RegionOfficeLocationResolver.find("서울 강남구")!!.point
        val seongnam = RegionOfficeLocationResolver.find("경기도 성남시")!!.point

        assertEquals(37.574771, jongno.lat, 0.000001)
        assertEquals(126.979612, jongno.lng, 0.000001)
        assertNotEquals(jongno, gangnam)
        assertNotEquals(gangnam, seongnam)
    }

    @Test
    fun `uses a zoom level that fits each municipality extent`() {
        val jongno = RegionOfficeLocationResolver.find("서울 종로구")!!
        val seongnam = RegionOfficeLocationResolver.find("경기도 성남시")!!
        val hongcheon = RegionOfficeLocationResolver.find("강원특별자치도 홍천군")!!

        assertEquals(12, jongno.zoomLevel)
        assertEquals(11, seongnam.zoomLevel)
        assertEquals(9, hongcheon.zoomLevel)
    }
}
