package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.BuildConfig
import com.dororong.rodi.core.data.mapper.toDomain
import com.dororong.rodi.core.data.mock.LocalTestPlaces
import com.dororong.rodi.core.data.source.local.datastore.SavedPlaceLocalDataSource
import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.remote.api.PlaceApi
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.CursorPage
import com.dororong.rodi.core.domain.model.place.PlaceCoordinate
import com.dororong.rodi.core.domain.model.place.PlaceDetail
import com.dororong.rodi.core.domain.model.place.PlaceException
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import com.dororong.rodi.core.domain.model.search.RelatedSearch
import com.dororong.rodi.core.domain.repository.PlaceRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

class PlaceRepositoryImpl @Inject constructor(
    private val api: PlaceApi,
    private val savedPlaceLocalDataSource: SavedPlaceLocalDataSource,
    private val tokenStore: AuthTokenStore,
) : PlaceRepository {
    override suspend fun getCoordinates(): List<PlaceCoordinate> {
        val coordinates = publicRequest {
            api.getCoordinates().requireData().map { it.toDomain() }
        }
        return if (BuildConfig.DEBUG) coordinates + LocalTestPlaces.coordinate() else coordinates
    }

    override suspend fun getPlaces(
        query: PlaceViewportQuery,
        cursor: String?,
        size: Int,
    ): CursorPage<PlaceSummary> {
        val page = optionalAuthenticatedRequest {
            api.getPlaces(
                swLat = query.southWest.lat,
                swLng = query.southWest.lng,
                neLat = query.northEast.lat,
                neLng = query.northEast.lng,
                lat = query.origin.lat,
                lng = query.origin.lng,
                size = size,
                cursor = cursor,
            ).requireData().toDomain()
        }
        val includeTestPlace = BuildConfig.DEBUG && cursor == null && LocalTestPlaces.containedIn(query)
        return if (includeTestPlace) {
            page.copy(
                items = listOf(LocalTestPlaces.summary()) + page.items,
                totalCount = page.totalCount?.plus(1),
            )
        } else {
            page
        }
    }

    override suspend fun searchPlaces(
        keyword: String,
        origin: GeoPoint,
        cursor: String?,
        size: Int,
    ): CursorPage<PlaceSummary> = authenticatedRequest {
        api.searchPlaces(
            keyword = keyword,
            lat = origin.lat,
            lng = origin.lng,
            size = size,
            cursor = cursor,
        ).requireData().toDomain()
    }

    override suspend fun relatedSearch(
        keyword: String,
        cursor: String?,
        size: Int,
    ): RelatedSearch = authenticatedRequest {
        api.relatedSearch(
            keyword = keyword,
            size = size,
            cursor = cursor,
        ).requireData().toDomain()
    }

    override suspend fun getPlaceDetail(placeId: Long): PlaceDetail {
        if (BuildConfig.DEBUG && placeId == LocalTestPlaces.ID) {
            val isBookmarked = savedPlaceLocalDataSource.observeSavedPlaces().first()
                .any { it.id == LocalTestPlaces.ID }
            return LocalTestPlaces.detail().copy(isBookmarked = isBookmarked)
        }
        return authenticatedRequest {
            api.getPlaceDetail(placeId).requireData().toDomain()
        }
    }

    override suspend fun getSavedPlaces(cursor: String?, size: Int): CursorPage<PlaceSummary> =
        authenticatedRequest {
            api.getSavedPlaces(size, cursor).requireData().toDomain()
        }

    override suspend fun setBookmarked(place: PlaceDetail, bookmarked: Boolean) {
        if (BuildConfig.DEBUG && place.id == LocalTestPlaces.ID) {
            savedPlaceLocalDataSource.setBookmarked(place, bookmarked)
            return
        }
        authenticatedRequest {
            val response = if (bookmarked) {
                api.bookmark(place.id)
            } else {
                api.unbookmark(place.id)
            }
            response.requireSuccess()
        }
        savedPlaceLocalDataSource.setBookmarked(place, bookmarked)
    }

    /**
     * 토큰 주입과 401 재발급은 OkHttp의 AuthHeaderInterceptor·TokenAuthenticator가 한다.
     * 여기서는 로그인 여부만 확인하고, 남은 실패를 도메인 예외로 바꾼다.
     */
    private suspend fun <T> authenticatedRequest(block: suspend () -> T): T {
        tokenStore.getTokens()?.accessToken ?: throw PlaceException.AuthenticationRequired("로그인이 필요합니다.")
        return try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            throw error.toPlaceException()
        }
    }


    /** 비로그인도 부를 수 있는 API. 토큰이 없으면 인터셉터가 헤더를 붙이지 않는다. */
    private suspend fun <T> optionalAuthenticatedRequest(block: suspend () -> T): T {
        if (tokenStore.getTokens()?.accessToken == null) return publicRequest { block() }
        return authenticatedRequest { block() }
    }

    private suspend fun <T> publicRequest(block: suspend () -> T): T = try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        throw error.toPlaceException()
    }
}

private fun <T> ApiEnvelope<T>.requireData(): T {
    if (!isSuccess) throw toPlaceException()
    return data ?: throw PlaceException.Unexpected(message.ifBlank { "응답 데이터가 없습니다." })
}

private fun ApiEnvelope<*>.requireSuccess() {
    if (!isSuccess) throw toPlaceException()
}

private fun ApiEnvelope<*>.toPlaceException(): PlaceException = when {
    code.contains("401") -> PlaceException.AuthenticationRequired(message)
    code.contains("404") -> PlaceException.NotFound(message)
    else -> PlaceException.Unexpected(message.ifBlank { "장소 요청에 실패했습니다." })
}

private fun Throwable.toPlaceException(): PlaceException = when (this) {
    is PlaceException -> this
    is HttpException -> when (code()) {
        401 -> PlaceException.AuthenticationRequired(message(), this)
        404 -> PlaceException.NotFound(message(), this)
        else -> PlaceException.Unexpected(message(), this)
    }
    is IOException -> PlaceException.Network("네트워크 연결을 확인해주세요.", this)
    else -> PlaceException.Unexpected(message ?: "장소 요청에 실패했습니다.", this)
}
