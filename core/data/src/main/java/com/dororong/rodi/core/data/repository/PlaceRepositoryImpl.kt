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
import com.dororong.rodi.core.domain.repository.AuthRepository
import com.dororong.rodi.core.domain.repository.PlaceRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

class PlaceRepositoryImpl @Inject constructor(
    private val api: PlaceApi,
    private val savedPlaceLocalDataSource: SavedPlaceLocalDataSource,
    private val tokenStore: AuthTokenStore,
    private val authRepository: AuthRepository,
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
        val page = optionalAuthenticatedRequest { accessToken ->
            api.getPlaces(
                authorization = accessToken?.let { "Bearer $it" },
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
    ): CursorPage<PlaceSummary> = authenticatedRequest { accessToken ->
        api.searchPlaces(
            authorization = "Bearer $accessToken",
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
    ): RelatedSearch = authenticatedRequest { accessToken ->
        api.relatedSearch(
            authorization = "Bearer $accessToken",
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
        return authenticatedRequest { accessToken ->
            api.getPlaceDetail("Bearer $accessToken", placeId).requireData().toDomain()
        }
    }

    override suspend fun getSavedPlaces(cursor: String?, size: Int): CursorPage<PlaceSummary> =
        authenticatedRequest { accessToken ->
            api.getSavedPlaces("Bearer $accessToken", size, cursor).requireData().toDomain()
        }

    override suspend fun setBookmarked(place: PlaceDetail, bookmarked: Boolean) {
        if (BuildConfig.DEBUG && place.id == LocalTestPlaces.ID) {
            savedPlaceLocalDataSource.setBookmarked(place, bookmarked)
            return
        }
        authenticatedRequest { accessToken ->
            val response = if (bookmarked) {
                api.bookmark("Bearer $accessToken", place.id)
            } else {
                api.unbookmark("Bearer $accessToken", place.id)
            }
            response.requireSuccess()
        }
        savedPlaceLocalDataSource.setBookmarked(place, bookmarked)
    }

    override fun observeSavedPlaces(): Flow<List<PlaceSummary>> = savedPlaceLocalDataSource.observeSavedPlaces()

    private suspend fun <T> authenticatedRequest(
        canRefresh: Boolean = true,
        block: suspend (String) -> T,
    ): T {
        val accessToken = tokenStore.getTokens()?.accessToken
            ?: throw PlaceException.AuthenticationRequired("로그인이 필요합니다.")
        return try {
            block(accessToken)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val mapped = error.toPlaceException()
            if (mapped is PlaceException.AuthenticationRequired && canRefresh) {
                refreshAndRetry(block)
            } else {
                throw mapped
            }
        }
    }

    private suspend fun <T> refreshAndRetry(block: suspend (String) -> T): T {
        try {
            authRepository.reissueToken()
        } catch (error: CancellationException) {
            throw error
        } catch (error: AuthException) {
            throw PlaceException.AuthenticationRequired(error.message ?: "로그인이 필요합니다.", error)
        }
        return authenticatedRequest(canRefresh = false, block = block)
    }

    private suspend fun <T> optionalAuthenticatedRequest(block: suspend (String?) -> T): T {
        if (tokenStore.getTokens()?.accessToken == null) {
            return publicRequest { block(null) }
        }
        return authenticatedRequest { accessToken -> block(accessToken) }
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
