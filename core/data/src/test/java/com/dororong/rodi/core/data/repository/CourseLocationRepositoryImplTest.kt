package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.datastore.CourseSearchHistoryDataStore
import com.dororong.rodi.core.data.source.remote.api.KakaoLocalApi
import com.dororong.rodi.core.data.source.remote.model.kakao.KakaoAddressDocument
import com.dororong.rodi.core.data.source.remote.model.kakao.KakaoAddressSearchResponse
import com.dororong.rodi.core.data.source.remote.model.kakao.KakaoCoordinateAddressResponse
import com.dororong.rodi.core.data.source.remote.model.kakao.KakaoKeywordDocument
import com.dororong.rodi.core.data.source.remote.model.kakao.KakaoKeywordSearchResponse
import com.dororong.rodi.core.data.source.remote.model.kakao.KakaoRoadAddress
import com.dororong.rodi.core.data.test.assertThrowsSuspend
import com.dororong.rodi.core.domain.model.course.CourseLocationResolution
import com.dororong.rodi.core.domain.model.course.CourseLocationSuggestionSource
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.course.CourseRegistrationException
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.search.PlaceSuggestion
import com.dororong.rodi.core.domain.model.search.RelatedSearch
import com.dororong.rodi.core.domain.repository.PlaceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CourseLocationRepositoryImplTest {
    @Test
    fun `does not resolve server place details until a result is selected`() = runTest {
        val places = mockk<PlaceRepository>()
        val api = mockk<KakaoLocalApi>()
        val history = mockk<CourseSearchHistoryDataStore>(relaxed = true)
        coEvery { places.relatedSearch("강남", null, 50) } returns RelatedSearch(
            regions = emptyList(),
            places = CursorPage(
                items = listOf(PlaceSuggestion(placeId = 7, name = "강남 연습장", region = "서울 강남구")),
                hasNext = false,
                nextCursor = null,
                totalCount = 1,
            ),
        )
        coEvery { api.searchKeyword(any(), "강남", 15) } returns KakaoKeywordSearchResponse()
        coEvery { api.searchAddress(any(), "강남", 15) } returns KakaoAddressSearchResponse()
        val repository = CourseLocationRepositoryImpl(places, api, history)

        val searchResult = repository.search("강남")
        val serverSuggestion = searchResult.places.single()

        assertNull(serverSuggestion.point)
        assertEquals(CourseLocationSuggestionSource.SERVER_PLACE, serverSuggestion.source)
        assertEquals(CourseLocationResolution.REQUIRES_PLACE_DETAIL, serverSuggestion.resolution)
        coVerify(exactly = 0) { places.getPlaceDetail(any()) }

        val detail = mockk<PlaceDetail>()
        every { detail.id } returns 7
        every { detail.name } returns "강남 연습장"
        every { detail.address } returns "서울 강남구 테헤란로"
        every { detail.point } returns GeoPoint(37.5, 127.0)
        coEvery { places.getPlaceDetail(7) } returns detail

        val resolved = repository.resolveSelection(serverSuggestion)

        assertEquals(GeoPoint(37.5, 127.0), resolved?.point)
        assertEquals("서울 강남구 테헤란로", resolved?.address)
        assertEquals(CourseLocationResolution.RESOLVED, resolved?.resolution)
        coVerify(exactly = 1) { places.getPlaceDetail(7) }
    }

    @Test
    fun `keeps server regions when Kakao address search fails and resolves them on selection`() = runTest {
        val places = mockk<PlaceRepository>()
        val api = mockk<KakaoLocalApi>()
        val history = mockk<CourseSearchHistoryDataStore>(relaxed = true)
        coEvery { places.relatedSearch("강남구", null, 50) } returns RelatedSearch(
            regions = listOf("서울 강남구"),
            places = CursorPage(emptyList(), false, null, 0),
        )
        coEvery { api.searchKeyword(any(), "강남구", 15) } returns KakaoKeywordSearchResponse()
        coEvery { api.searchAddress(any(), "강남구", 15) } throws IllegalStateException("address down")
        val repository = CourseLocationRepositoryImpl(places, api, history)

        val searchResult = repository.search("강남구")
        val serverRegion = searchResult.regions.single()

        assertEquals("서울 강남구", serverRegion.address)
        assertNull(serverRegion.point)
        assertEquals(CourseLocationSuggestionSource.SERVER_REGION, serverRegion.source)
        assertTrue(searchResult.isPartial)

        coEvery { api.searchAddress(any(), "서울 강남구", 15) } returns KakaoAddressSearchResponse(
            documents = listOf(
                KakaoAddressDocument(
                    addressName = "서울 강남구",
                    roadAddress = KakaoRoadAddress("서울 강남구 테헤란로"),
                    x = "127.0",
                    y = "37.5",
                ),
            ),
        )
        val resolved = repository.resolveSelection(serverRegion)

        assertEquals(GeoPoint(37.5, 127.0), resolved?.point)
        assertEquals(CourseLocationResolution.RESOLVED, resolved?.resolution)
        coVerify(exactly = 1) { api.searchAddress(any(), "서울 강남구", 15) }
        coVerify(exactly = 0) { places.getPlaceDetail(any()) }
    }

    @Test
    fun `keeps Kakao keyword results when server related search is unavailable`() = runTest {
        val places = mockk<PlaceRepository>()
        val api = mockk<KakaoLocalApi>()
        val history = mockk<CourseSearchHistoryDataStore>(relaxed = true)
        coEvery { places.relatedSearch("교차로", null, 50) } throws IllegalStateException("server down")
        coEvery { api.searchKeyword("KakaoAK ${com.dororong.rodi.core.data.BuildConfig.KAKAO_REST_API_KEY}", "교차로", 15) } returns KakaoKeywordSearchResponse(
            documents = listOf(KakaoKeywordDocument("1", "교차로", "서울", "서울 도로", "127.0", "37.0")),
        )
        coEvery {
            api.searchAddress("KakaoAK ${com.dororong.rodi.core.data.BuildConfig.KAKAO_REST_API_KEY}", "교차로", 15)
        } returns KakaoAddressSearchResponse()
        val repository = CourseLocationRepositoryImpl(places, api, history)

        val result = repository.search("교차로")

        assertEquals("교차로", result.places.single().title)
        assertTrue(result.isPartial)
    }

    @Test
    fun `prefers road address during reverse geocoding`() = runTest {
        val places = mockk<PlaceRepository>()
        val api = mockk<KakaoLocalApi>()
        val history = mockk<CourseSearchHistoryDataStore>(relaxed = true)
        coEvery { api.reverseGeocode("KakaoAK ${com.dororong.rodi.core.data.BuildConfig.KAKAO_REST_API_KEY}", 127.0, 37.0, "WGS84") } returns
            KakaoCoordinateAddressResponse(
                documents = listOf(
                    com.dororong.rodi.core.data.source.remote.model.kakao.KakaoCoordinateAddressDocument(
                        address = com.dororong.rodi.core.data.source.remote.model.kakao.KakaoJibunAddress("지번 주소"),
                        roadAddress = KakaoRoadAddress("도로명 주소"),
                    ),
                ),
            )
        val repository = CourseLocationRepositoryImpl(places, api, history)

        val result = repository.reverseGeocode(GeoPoint(37.0, 127.0))

        assertEquals("도로명 주소", result?.address)
    }

    @Test
    fun `does not swallow cancellation from any parallel search source`() = runTest {
        val places = mockk<PlaceRepository>()
        val api = mockk<KakaoLocalApi>()
        val history = mockk<CourseSearchHistoryDataStore>(relaxed = true)
        coEvery { places.relatedSearch("교차로", null, 50) } throws CancellationException("cancelled")
        coEvery { api.searchKeyword(any(), "교차로", 15) } returns KakaoKeywordSearchResponse()
        coEvery { api.searchAddress(any(), "교차로", 15) } returns KakaoAddressSearchResponse()
        val repository = CourseLocationRepositoryImpl(places, api, history)

        assertThrowsSuspend<CancellationException> { repository.search("교차로") }
    }

    @Test
    fun `returns search error only when every source fails`() = runTest {
        val places = mockk<PlaceRepository>()
        val api = mockk<KakaoLocalApi>()
        val history = mockk<CourseSearchHistoryDataStore>(relaxed = true)
        coEvery { places.relatedSearch("교차로", null, 50) } throws IllegalStateException("server down")
        coEvery { api.searchKeyword(any(), "교차로", 15) } throws IllegalStateException("keyword down")
        coEvery { api.searchAddress(any(), "교차로", 15) } throws IllegalStateException("address down")
        val repository = CourseLocationRepositoryImpl(places, api, history)

        assertThrowsSuspend<CourseRegistrationException.SearchUnavailable> { repository.search("교차로") }
    }
}
