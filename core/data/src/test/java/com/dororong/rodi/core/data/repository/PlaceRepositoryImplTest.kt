package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.common.userMessage
import com.dororong.rodi.core.data.di.NetworkModule
import com.dororong.rodi.core.data.source.local.datastore.SavedPlaceLocalDataSource
import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.local.security.AuthTokens
import com.dororong.rodi.core.data.source.remote.api.PlaceApi
import com.dororong.rodi.core.data.source.remote.model.place.PlaceDetailResponse
import com.dororong.rodi.core.data.source.remote.model.place.CursorPagePlaceResponse
import com.dororong.rodi.core.data.source.remote.model.place.PlaceListItemResponse
import com.dororong.rodi.core.data.source.remote.model.place.CursorPagePlaceSuggestionResponse
import com.dororong.rodi.core.data.source.remote.model.place.PlaceSuggestionResponse
import com.dororong.rodi.core.data.source.remote.model.place.RelatedSearchResponse
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.data.test.assertThrowsSuspend
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceException
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

class PlaceRepositoryImplTest {
    @Test
    fun `연관 검색은 서버 지역과 장소 커서를 매핑한다`() = runTest {
        val api = mockk<PlaceApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens("access")
        coEvery { api.relatedSearch("중구", 20, null) } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = RelatedSearchResponse(
                regions = listOf("부산 중구", "서울 중구"),
                places = CursorPagePlaceSuggestionResponse(
                    items = listOf(PlaceSuggestionResponse(7, "중구 연습 코스", "서울 중구")),
                    hasNext = true,
                    nextCursor = "next-7",
                    totalCount = 21,
                ),
            ),
        )
        val repository = PlaceRepositoryImpl(
            api,
            mockk<SavedPlaceLocalDataSource>(relaxed = true),
            tokenStore,
            NetworkModule.provideJson(),
        )

        val result = repository.relatedSearch("중구", cursor = null, size = 20)

        assertEquals(listOf("부산 중구", "서울 중구"), result.regions)
        assertEquals(7L, result.places.items.single().placeId)
        assertEquals("next-7", result.places.nextCursor)
        coVerify(exactly = 1) { api.relatedSearch("중구", 20, null) }
    }

    @Test
    fun `세션이 있으면 장소 목록을 조회 조건 그대로 요청한다`() = runTest {
        val api = mockk<PlaceApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val query = viewportQuery()
        coEvery { tokenStore.getTokens() } returns tokens("access")
        coEvery {
            api.getPlaces(
                swLat = query.southWest.lat,
                swLng = query.southWest.lng,
                neLat = query.northEast.lat,
                neLng = query.northEast.lng,
                lat = query.origin.lat,
                lng = query.origin.lng,
                size = 20,
                cursor = null,
            )
        } returns placePageEnvelope()
        val repository = PlaceRepositoryImpl(
            api,
            mockk<SavedPlaceLocalDataSource>(relaxed = true),
            tokenStore,
            NetworkModule.provideJson(),
        )

        repository.getPlaces(query, cursor = null, size = 20)

        coVerify(exactly = 1) {
            api.getPlaces(
                swLat = query.southWest.lat,
                swLng = query.southWest.lng,
                neLat = query.northEast.lat,
                neLng = query.northEast.lng,
                lat = query.origin.lat,
                lng = query.origin.lng,
                size = 20,
                cursor = null,
            )
        }
    }

    @Test
    fun `저장 목록은 없을 수 있는 거리와 커서 페이지 정보를 유지한다`() = runTest {
        val api = mockk<PlaceApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens("access")
        coEvery { api.getSavedPlaces(20, null) } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = CursorPagePlaceResponse(
                items = listOf(
                    PlaceListItemResponse(
                        id = 3,
                        type = "PARKING",
                        name = "주차장",
                        address = "서울",
                        lat = 37.5,
                        lng = 126.9,
                        distanceFromMe = null,
                        practiceTypes = listOf("PARKING"),
                        isDeleted = false,
                    ),
                ),
                hasNext = true,
                nextCursor = "next-3",
                totalCount = 21,
            ),
        )
        val repository = PlaceRepositoryImpl(
            api,
            mockk<SavedPlaceLocalDataSource>(relaxed = true),
            tokenStore,
            NetworkModule.provideJson(),
        )

        val page = repository.getSavedPlaces(cursor = null, size = 20)

        assertEquals(null, page.items.single().distanceFromMeMeters)
        assertEquals("next-3", page.nextCursor)
        assertEquals(21, page.totalCount)
    }

    @Test
    fun `북마크는 서버가 성공한 뒤에만 로컬 캐시를 갱신한다`() = runTest {
        val api = mockk<PlaceApi>()
        val local = mockk<SavedPlaceLocalDataSource>(relaxed = true)
        val tokenStore = mockk<AuthTokenStore>()
        val authRepository = mockk<AuthRepository>()
        coEvery { tokenStore.getTokens() } returns tokens("access")
        coEvery { api.bookmark(9) } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = buildJsonObject { },
        )
        val repository = PlaceRepositoryImpl(api, local, tokenStore, NetworkModule.provideJson())
        val place = place(9)

        repository.setBookmarked(place, true)

        coVerify(exactly = 1) { local.setBookmarked(place, true) }
    }

    @Test
    fun `북마크 요청이 실패하면 로컬 캐시를 바꾸지 않는다`() = runTest {
        val api = mockk<PlaceApi>()
        val local = mockk<SavedPlaceLocalDataSource>(relaxed = true)
        val tokenStore = mockk<AuthTokenStore>()
        val authRepository = mockk<AuthRepository>()
        coEvery { tokenStore.getTokens() } returns tokens("access")
        coEvery { api.bookmark(9) } returns failureEnvelope("COMMON_500")
        val repository = PlaceRepositoryImpl(api, local, tokenStore, NetworkModule.provideJson())
        val place = place(9)

        assertThrowsSuspend<RuntimeException> { repository.setBookmarked(place, true) }

        coVerify(exactly = 0) { local.setBookmarked(any(), any()) }
    }

    @Test
    fun `북마크 중 취소는 부수 효과 없이 다시 던진다`() = runTest {
        val api = mockk<PlaceApi>()
        val local = mockk<SavedPlaceLocalDataSource>(relaxed = true)
        val tokenStore = mockk<AuthTokenStore>()
        val authRepository = mockk<AuthRepository>(relaxed = true)
        coEvery { tokenStore.getTokens() } returns tokens("access")
        coEvery { api.bookmark(9) } throws CancellationException()
        val repository = PlaceRepositoryImpl(api, local, tokenStore, NetworkModule.provideJson())

        assertThrowsSuspend<CancellationException> { repository.setBookmarked(place(9), true) }

        coVerify(exactly = 0) { authRepository.reissueToken() }
        coVerify(exactly = 0) { local.setBookmarked(any(), any()) }
    }

    @Test
    fun `응답 필수 필드가 누락돼도 직렬화 오류 문구를 사용자에게 노출하지 않는다`() = runTest {
        val api = mockk<PlaceApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens("access")
        val missingField = runCatching {
            NetworkModule.provideJson().decodeFromString<ApiEnvelope<PlaceDetailResponse>>(
                """{"isSuccess":true,"code":"COMMON_200","message":"성공","data":""" +
                    """{"id":9,"type":"PARKING","name":"주차장","address":"서울","lat":37.5,"lng":126.9,""" +
                    """"practiceTypes":["PARKING"],"isBookmarked":false,"course":null,"parking":null}}""",
            )
        }.exceptionOrNull()
        coEvery { api.getPlaceDetail(9) } throws requireNotNull(missingField)
        val repository = PlaceRepositoryImpl(api, mockk(relaxed = true), tokenStore, NetworkModule.provideJson())

        val error = assertThrowsSuspend<PlaceException.Unexpected> { repository.getPlaceDetail(9) }

        assertEquals("장소 요청에 실패했습니다.", error.userMessage())
    }

    @Test
    fun `목록을 받은 뒤 삭제된 코스의 상세를 열면 삭제됐다고 알린다`() = runTest {
        val api = mockk<PlaceApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens("access")
        coEvery { api.getPlaceDetail(9) } throws HttpException(
            Response.error<Any>(
                404,
                """{"isSuccess":false,"code":"COURSE_404_2","message":"삭제된 코스입니다.","data":null}"""
                    .toResponseBody("application/json".toMediaType()),
            ),
        )
        val repository = PlaceRepositoryImpl(api, mockk(relaxed = true), tokenStore, NetworkModule.provideJson())

        val error = assertThrowsSuspend<PlaceException.NotFound> { repository.getPlaceDetail(9) }

        assertEquals("삭제된 코스예요.", error.userMessage())
    }

    private fun tokens(access: String) = AuthTokens(access, "refresh", "kakao")

    private fun viewportQuery() = com.dororong.rodi.core.domain.model.place.PlaceViewportQuery(
        southWest = GeoPoint(37.4, 126.8),
        northEast = GeoPoint(37.6, 127.0),
        origin = GeoPoint(37.5, 126.9),
    )

    private fun placePageEnvelope() = ApiEnvelope(
        isSuccess = true,
        code = "COMMON_200",
        message = "성공",
        data = CursorPagePlaceResponse(items = emptyList(), hasNext = false),
    )

    private fun detailEnvelope(id: Long) = ApiEnvelope(
        isSuccess = true,
        code = "COMMON_200",
        message = "성공",
        data = PlaceDetailResponse(id, "PARKING", "주차장", "서울", 37.5, 126.9, practiceTypes = listOf("PARKING"), bookmarkCount = 0, isBookmarked = false),
    )

    private fun <T> failureEnvelope(code: String): ApiEnvelope<T> = ApiEnvelope(
        isSuccess = false,
        code = code,
        message = "실패",
    )

    private fun place(id: Long) = PlaceDetail(
        id = id,
        type = PlaceType.PARKING,
        name = "주차장",
        address = "서울",
        point = GeoPoint(37.5, 126.9),
        practiceTypes = emptyList(),
        bookmarkCount = 0,
        isBookmarked = false,
        course = null,
        parking = null,
    )
}
