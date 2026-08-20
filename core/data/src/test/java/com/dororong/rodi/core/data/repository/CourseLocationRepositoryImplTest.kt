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
import com.dororong.rodi.core.domain.repository.PlaceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CourseLocationRepositoryImplTest {
    @Test
    fun `searches only Kakao so Rodi course names never show up`() = runTest {
        val places = mockk<PlaceRepository>()
        val api = mockk<KakaoLocalApi>()
        val history = mockk<CourseSearchHistoryDataStore>(relaxed = true)
        coEvery { api.searchKeyword(any(), "강남", 15) } returns KakaoKeywordSearchResponse(
            documents = listOf(KakaoKeywordDocument("1", "강남 연습장", "서울 강남구", "서울 강남구 테헤란로", "127.0", "37.5")),
        )
        coEvery { api.searchAddress(any(), "강남", 15) } returns KakaoAddressSearchResponse()
        val repository = CourseLocationRepositoryImpl(places, api, history)

        val searchResult = repository.search("강남")
        val suggestion = searchResult.places.single()

        assertEquals("강남 연습장", suggestion.title)
        assertEquals(CourseLocationSuggestionSource.KAKAO_KEYWORD, suggestion.source)
        assertEquals(CourseLocationResolution.RESOLVED, suggestion.resolution)
        coVerify(exactly = 0) { places.relatedSearch(any(), any(), any()) }
        coVerify(exactly = 0) { places.getPlaceDetail(any()) }
    }

    @Test
    fun `maps Kakao address results to regions with a resolved point`() = runTest {
        val places = mockk<PlaceRepository>()
        val api = mockk<KakaoLocalApi>()
        val history = mockk<CourseSearchHistoryDataStore>(relaxed = true)
        coEvery { api.searchKeyword(any(), "강남구", 15) } returns KakaoKeywordSearchResponse()
        coEvery { api.searchAddress(any(), "강남구", 15) } returns KakaoAddressSearchResponse(
            documents = listOf(
                KakaoAddressDocument(
                    addressName = "서울 강남구",
                    roadAddress = KakaoRoadAddress("서울 강남구 테헤란로"),
                    x = "127.0",
                    y = "37.5",
                ),
            ),
        )
        val repository = CourseLocationRepositoryImpl(places, api, history)

        val region = repository.search("강남구").regions.single()

        assertEquals("서울 강남구 테헤란로", region.address)
        assertEquals(GeoPoint(37.5, 127.0), region.point)
        assertEquals(CourseLocationResolution.RESOLVED, region.resolution)
    }

    @Test
    fun `keeps keyword results when address search is unavailable`() = runTest {
        val places = mockk<PlaceRepository>()
        val api = mockk<KakaoLocalApi>()
        val history = mockk<CourseSearchHistoryDataStore>(relaxed = true)
        coEvery { api.searchKeyword(any(), "교차로", 15) } returns KakaoKeywordSearchResponse(
            documents = listOf(KakaoKeywordDocument("1", "교차로", "서울", "서울 도로", "127.0", "37.0")),
        )
        coEvery { api.searchAddress(any(), "교차로", 15) } throws IllegalStateException("address down")
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
        coEvery { api.searchKeyword(any(), "교차로", 15) } throws CancellationException("cancelled")
        coEvery { api.searchAddress(any(), "교차로", 15) } returns KakaoAddressSearchResponse()
        val repository = CourseLocationRepositoryImpl(places, api, history)

        assertThrowsSuspend<CancellationException> { repository.search("교차로") }
    }

    @Test
    fun `returns search error only when every source fails`() = runTest {
        val places = mockk<PlaceRepository>()
        val api = mockk<KakaoLocalApi>()
        val history = mockk<CourseSearchHistoryDataStore>(relaxed = true)
        coEvery { api.searchKeyword(any(), "교차로", 15) } throws IllegalStateException("keyword down")
        coEvery { api.searchAddress(any(), "교차로", 15) } throws IllegalStateException("address down")
        val repository = CourseLocationRepositoryImpl(places, api, history)

        assertThrowsSuspend<CourseRegistrationException.SearchUnavailable> { repository.search("교차로") }
    }
}
