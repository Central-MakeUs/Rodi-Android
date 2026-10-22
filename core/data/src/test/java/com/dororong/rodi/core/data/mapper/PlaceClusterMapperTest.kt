package com.dororong.rodi.core.data.mapper

import com.dororong.rodi.core.data.di.NetworkModule
import com.dororong.rodi.core.data.source.remote.model.place.PlaceMarkerClustersResponse
import com.dororong.rodi.core.domain.model.course.GeoPoint
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PlaceClusterMapperTest {
    @Test
    fun `클러스터 응답은 단계 범위 개별 장소와 탭할 bounds를 보존한다`() {
        val result = response().toDomain()

        assertEquals(listOf(1L, 2L), result.places.map { it.id })
        assertEquals(0.1, result.levels.first().minLngSpan)
        assertNull(result.levels.first().maxLngSpan)
        val cluster = result.levels.first().clusters.single()
        assertEquals(2, cluster.count)
        assertEquals(GeoPoint(37.55, 126.95), cluster.point)
        assertEquals(GeoPoint(37.5, 126.9), cluster.bounds.southWest)
        assertEquals(GeoPoint(37.6, 127.0), cluster.bounds.northEast)
        assertEquals(listOf(1L, 2L), result.levels.last().placeIds)
    }

    @Test
    fun `클러스터 단계의 필수 필드를 기본값으로 숨기지 않는다`() {
        assertThrows<SerializationException> {
            NetworkModule.provideJson().decodeFromString<PlaceMarkerClustersResponse>(
                """{"places":[],"levels":[{"minLngSpan":0,"maxLngSpan":null,"clusters":[]}]}""",
            )
        }
    }

    @Test
    fun `존재하지 않는 개별 장소 id를 참조하면 실패한다`() {
        val response = response()
        assertThrows<IllegalArgumentException> {
            response.copy(levels = listOf(response.levels.first(), response.levels.last().copy(placeIds = listOf(1, 999)))).toDomain()
        }
    }

    @Test
    fun `클러스터와 개별 장소 수가 전체 장소 수와 다르면 실패한다`() {
        val response = response()
        assertThrows<IllegalArgumentException> {
            response.copy(levels = listOf(response.levels.first(), response.levels.last().copy(placeIds = listOf(1)))).toDomain()
        }
    }

    @Test
    fun `뒤집힌 bounds와 잘못된 단계 범위는 실패한다`() {
        val response = response()
        val level = response.levels.first()
        val cluster = level.clusters.single()
        assertThrows<IllegalArgumentException> {
            response.copy(levels = listOf(level.copy(clusters = listOf(
                cluster.copy(bounds = cluster.bounds.copy(swLat = 38.0)),
            )), response.levels.last())).toDomain()
        }
        assertThrows<IllegalArgumentException> {
            response.copy(levels = listOf(level, response.levels.last().copy(maxLngSpan = 0.0))).toDomain()
        }
    }

    @Test
    fun `클러스터 단계에 빈 구간이나 확대 범위 누락이 있으면 실패한다`() {
        val response = response()
        assertThrows<IllegalArgumentException> {
            response.copy(levels = listOf(response.levels.first())).toDomain()
        }
        assertThrows<IllegalArgumentException> {
            response.copy(levels = listOf(
                response.levels.first(),
                response.levels.last().copy(maxLngSpan = 0.05),
            )).toDomain()
        }
    }

    private fun response() = NetworkModule.provideJson().decodeFromString<PlaceMarkerClustersResponse>(
        """{
          "places":[
            {"id":1,"type":"COURSE","name":"코스","address":"서울","lat":37.5,"lng":126.9},
            {"id":2,"type":"PARKING","name":"주차장","address":"서울","lat":37.6,"lng":127.0}
          ],
          "levels":[
            {"minLngSpan":0.1,"maxLngSpan":null,"clusters":[
              {"lat":37.55,"lng":126.95,"count":2,"bounds":{"swLat":37.5,"swLng":126.9,"neLat":37.6,"neLng":127.0}}
            ],"placeIds":[]},
            {"minLngSpan":0.0,"maxLngSpan":0.1,"clusters":[],"placeIds":[1,2]}
          ]
        }""",
    )
}
