package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.datastore.SavedPlaceLocalDataSource
import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.local.security.AuthTokens
import com.dororong.rodi.core.data.source.remote.api.PlaceApi
import com.dororong.rodi.core.data.source.remote.model.place.PlaceDetailResponse
import com.dororong.rodi.core.data.source.remote.model.place.CursorPagePlaceResponse
import com.dororong.rodi.core.data.source.remote.model.place.PlaceListItemResponse
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.data.test.assertThrowsSuspend
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceType
import com.dororong.rodi.core.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlaceRepositoryImplTest {
    @Test
    fun `places sends access token when a session exists`() = runTest {
        val api = mockk<PlaceApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val query = viewportQuery()
        coEvery { tokenStore.getTokens() } returns tokens("access")
        coEvery {
            api.getPlaces(
                authorization = "Bearer access",
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
            mockk<AuthRepository>(),
        )

        repository.getPlaces(query, cursor = null, size = 20)

        coVerify(exactly = 1) {
            api.getPlaces(
                authorization = "Bearer access",
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
    fun `saved places preserves nullable distance and cursor page metadata`() = runTest {
        val api = mockk<PlaceApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns tokens("access")
        coEvery { api.getSavedPlaces("Bearer access", 20, null) } returns ApiEnvelope(
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
            mockk<AuthRepository>(),
        )

        val page = repository.getSavedPlaces(cursor = null, size = 20)

        assertEquals(null, page.items.single().distanceFromMeMeters)
        assertEquals("next-3", page.nextCursor)
        assertEquals(21, page.totalCount)
    }

    @Test
    fun `detail retries once after access token refresh`() = runTest {
        val api = mockk<PlaceApi>()
        val local = mockk<SavedPlaceLocalDataSource>(relaxed = true)
        val tokenStore = mockk<AuthTokenStore>()
        val authRepository = mockk<AuthRepository>()
        coEvery { tokenStore.getTokens() } returnsMany listOf(tokens("old"), tokens("new"))
        coEvery { api.getPlaceDetail("Bearer old", 7) } returns failureEnvelope("COMMON_401")
        coEvery { authRepository.reissueToken() } returns Unit
        coEvery { api.getPlaceDetail("Bearer new", 7) } returns detailEnvelope(7)
        val repository = PlaceRepositoryImpl(api, local, tokenStore, authRepository)

        val detail = repository.getPlaceDetail(7)

        assertEquals(7, detail.id)
        coVerify(exactly = 1) { authRepository.reissueToken() }
        coVerify(exactly = 1) { api.getPlaceDetail("Bearer new", 7) }
    }

    @Test
    fun `bookmark updates local cache only after server success`() = runTest {
        val api = mockk<PlaceApi>()
        val local = mockk<SavedPlaceLocalDataSource>(relaxed = true)
        val tokenStore = mockk<AuthTokenStore>()
        val authRepository = mockk<AuthRepository>()
        coEvery { tokenStore.getTokens() } returns tokens("access")
        coEvery { api.bookmark("Bearer access", 9) } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = buildJsonObject { },
        )
        val repository = PlaceRepositoryImpl(api, local, tokenStore, authRepository)
        val place = place(9)

        repository.setBookmarked(place, true)

        coVerify(exactly = 1) { local.setBookmarked(place, true) }
    }

    @Test
    fun `bookmark failure leaves local cache unchanged`() = runTest {
        val api = mockk<PlaceApi>()
        val local = mockk<SavedPlaceLocalDataSource>(relaxed = true)
        val tokenStore = mockk<AuthTokenStore>()
        val authRepository = mockk<AuthRepository>()
        coEvery { tokenStore.getTokens() } returns tokens("access")
        coEvery { api.bookmark("Bearer access", 9) } returns failureEnvelope("COMMON_500")
        val repository = PlaceRepositoryImpl(api, local, tokenStore, authRepository)
        val place = place(9)

        assertThrowsSuspend<RuntimeException> { repository.setBookmarked(place, true) }

        coVerify(exactly = 0) { local.setBookmarked(any(), any()) }
    }

    @Test
    fun `bookmark cancellation is rethrown without side effects`() = runTest {
        val api = mockk<PlaceApi>()
        val local = mockk<SavedPlaceLocalDataSource>(relaxed = true)
        val tokenStore = mockk<AuthTokenStore>()
        val authRepository = mockk<AuthRepository>(relaxed = true)
        coEvery { tokenStore.getTokens() } returns tokens("access")
        coEvery { api.bookmark("Bearer access", 9) } throws CancellationException()
        val repository = PlaceRepositoryImpl(api, local, tokenStore, authRepository)

        assertThrowsSuspend<CancellationException> { repository.setBookmarked(place(9), true) }

        coVerify(exactly = 0) { authRepository.reissueToken() }
        coVerify(exactly = 0) { local.setBookmarked(any(), any()) }
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
        data = CursorPagePlaceResponse(),
    )

    private fun detailEnvelope(id: Long) = ApiEnvelope(
        isSuccess = true,
        code = "COMMON_200",
        message = "성공",
        data = PlaceDetailResponse(id, "PARKING", "주차장", "서울", 37.5, 126.9),
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
