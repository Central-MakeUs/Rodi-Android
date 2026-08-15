package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.BuildConfig
import com.dororong.rodi.core.data.source.local.datastore.CourseSearchHistoryDataStore
import com.dororong.rodi.core.data.source.remote.api.KakaoLocalApi
import com.dororong.rodi.core.data.source.remote.model.kakao.KakaoAddressDocument
import com.dororong.rodi.core.data.source.remote.model.kakao.KakaoCoordinateAddressDocument
import com.dororong.rodi.core.data.source.remote.model.kakao.KakaoKeywordDocument
import com.dororong.rodi.core.domain.model.course.CourseLocationKind
import com.dororong.rodi.core.domain.model.course.CourseLocationResolution
import com.dororong.rodi.core.domain.model.course.CourseLocationSearchResult
import com.dororong.rodi.core.domain.model.course.CourseLocationSuggestion
import com.dororong.rodi.core.domain.model.course.CourseLocationSuggestionSource
import com.dororong.rodi.core.domain.model.course.CourseRegistrationException
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.repository.CourseLocationRepository
import com.dororong.rodi.core.domain.repository.PlaceRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow

class CourseLocationRepositoryImpl @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val kakaoLocalApi: KakaoLocalApi,
    private val historyDataStore: CourseSearchHistoryDataStore,
) : CourseLocationRepository {
    override fun observeRecent(): Flow<List<CourseLocationSuggestion>> = historyDataStore.observe()

    override suspend fun search(keyword: String): CourseLocationSearchResult {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return CourseLocationSearchResult()

        val outcomes = coroutineScope {
            val related = async { safeCall { placeRepository.relatedSearch(trimmed, null, 50) } }
            val keywords = async { safeCall { kakaoLocalApi.searchKeyword(kakaoAuthorization(), trimmed) } }
            val addresses = async { safeCall { kakaoLocalApi.searchAddress(kakaoAuthorization(), trimmed) } }
            Triple(related.await(), keywords.await(), addresses.await())
        }
        if (outcomes.toList().all(Result<*>::isFailure)) {
            throw CourseRegistrationException.SearchUnavailable(
                outcomes.toList().firstNotNullOfOrNull(Result<*>::exceptionOrNull),
            )
        }

        val related = outcomes.first.getOrNull()
        val keywordDocuments = outcomes.second.getOrNull()?.documents.orEmpty()
        val addressDocuments = outcomes.third.getOrNull()?.documents.orEmpty()

        val kakaoRegions = addressDocuments.mapNotNull(::toRegionSuggestion)
        val serverRegions = related?.regions.orEmpty().map(::toServerRegionSuggestion)
        val regions = deduplicate(serverRegions + kakaoRegions).take(4)

        val kakaoPlaces = keywordDocuments.mapNotNull(::toPlaceSuggestion)
        val addressPlaces = addressDocuments
            .mapNotNull(::toAddressPlaceSuggestion)
            .filterNot { address -> regions.any { it.address == address.address } }
        val serverPlaces = related?.places?.items.orEmpty().map(::toServerPlaceSuggestion)
        val places = deduplicate(serverPlaces + kakaoPlaces + addressPlaces).take(20)

        return CourseLocationSearchResult(
            regions = regions,
            places = places,
            isPartial = outcomes.toList().any(Result<*>::isFailure),
        )
    }

    override suspend fun resolveSelection(suggestion: CourseLocationSuggestion): CourseLocationSuggestion? {
        if (suggestion.resolution == CourseLocationResolution.RESOLVED && suggestion.point != null) {
            return suggestion
        }
        return when (suggestion.resolution) {
            CourseLocationResolution.REQUIRES_PLACE_DETAIL -> resolveServerPlace(suggestion)
            CourseLocationResolution.REQUIRES_REGION_CENTER -> resolveRegionCenter(suggestion)
            CourseLocationResolution.RESOLVED -> throw CourseRegistrationException.SelectionUnavailable()
        }
    }

    override suspend fun reverseGeocode(point: GeoPoint): CourseLocationSuggestion? {
        val document = try {
            kakaoLocalApi.reverseGeocode(kakaoAuthorization(), point.lng, point.lat).documents.firstOrNull()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            throw CourseRegistrationException.AddressUnavailable(error)
        } ?: return null
        val address = document.preferredAddress() ?: return null
        return CourseLocationSuggestion(
            id = "reverse-${point.lat}-${point.lng}",
            title = address,
            address = address,
            point = point,
            kind = CourseLocationKind.PLACE,
            source = CourseLocationSuggestionSource.REVERSE_GEOCODE,
            resolution = CourseLocationResolution.RESOLVED,
        )
    }

    override suspend fun saveRecent(suggestion: CourseLocationSuggestion) = historyDataStore.save(suggestion)

    override suspend fun deleteRecent(id: String) = historyDataStore.delete(id)

    override suspend fun clearRecent() = historyDataStore.clear()

    private suspend fun resolveServerPlace(suggestion: CourseLocationSuggestion): CourseLocationSuggestion {
        val placeId = suggestion.sourceId
            ?: suggestion.id.removePrefix(SERVER_PLACE_ID_PREFIX).toLongOrNull()
            ?: throw CourseRegistrationException.SelectionUnavailable()
        val detail = try {
            placeRepository.getPlaceDetail(placeId)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            throw CourseRegistrationException.SelectionUnavailable(error)
        }
        return suggestion.copy(
            id = "$SERVER_PLACE_ID_PREFIX${detail.id}",
            title = detail.name,
            address = detail.address,
            point = detail.point,
            sourceId = detail.id,
            resolution = CourseLocationResolution.RESOLVED,
        )
    }

    private suspend fun resolveRegionCenter(suggestion: CourseLocationSuggestion): CourseLocationSuggestion {
        val query = suggestion.address.ifBlank { suggestion.title }
        val response = try {
            kakaoLocalApi.searchAddress(kakaoAuthorization(), query)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            throw CourseRegistrationException.SelectionUnavailable(error)
        }
        val resolved = response.documents
            .mapNotNull(::toRegionSuggestion)
            .firstOrNull()
            ?: throw CourseRegistrationException.SelectionUnavailable()
        val point = resolved.point ?: throw CourseRegistrationException.SelectionUnavailable()
        return suggestion.copy(
            address = resolved.address,
            point = point,
            resolution = CourseLocationResolution.RESOLVED,
        )
    }

    private fun kakaoAuthorization(): String = "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}"

    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }

    private fun toServerRegionSuggestion(region: String): CourseLocationSuggestion {
        val normalized = region.trim()
        return CourseLocationSuggestion(
            id = "server-region-${normalized.hashCode()}",
            title = normalized.substringAfterLast(' ').ifBlank { normalized },
            address = normalized,
            point = null,
            kind = CourseLocationKind.REGION,
            source = CourseLocationSuggestionSource.SERVER_REGION,
            resolution = CourseLocationResolution.REQUIRES_REGION_CENTER,
        )
    }

    private fun toServerPlaceSuggestion(suggestion: com.dororong.rodi.core.domain.model.search.PlaceSuggestion) =
        CourseLocationSuggestion(
            id = "$SERVER_PLACE_ID_PREFIX${suggestion.placeId}",
            title = suggestion.name,
            address = suggestion.region,
            point = null,
            kind = CourseLocationKind.PLACE,
            source = CourseLocationSuggestionSource.SERVER_PLACE,
            resolution = CourseLocationResolution.REQUIRES_PLACE_DETAIL,
            sourceId = suggestion.placeId,
        )

    private fun toRegionSuggestion(document: KakaoAddressDocument): CourseLocationSuggestion? {
        val address = document.roadAddress?.addressName ?: document.addressName
        val point = document.toPoint() ?: return null
        return CourseLocationSuggestion(
            id = "kakao-region-${document.x}-${document.y}",
            title = address.substringAfterLast(' ').ifBlank { address },
            address = address,
            point = point,
            kind = CourseLocationKind.REGION,
            source = CourseLocationSuggestionSource.KAKAO_ADDRESS,
            resolution = CourseLocationResolution.RESOLVED,
        )
    }

    private fun toAddressPlaceSuggestion(document: KakaoAddressDocument): CourseLocationSuggestion? {
        val address = document.roadAddress?.addressName ?: document.addressName
        val point = document.toPoint() ?: return null
        return CourseLocationSuggestion(
            id = "kakao-address-${document.x}-${document.y}",
            title = address,
            address = address,
            point = point,
            kind = CourseLocationKind.PLACE,
            source = CourseLocationSuggestionSource.KAKAO_ADDRESS,
            resolution = CourseLocationResolution.RESOLVED,
        )
    }

    private fun toPlaceSuggestion(document: KakaoKeywordDocument): CourseLocationSuggestion? {
        val point = document.toPoint() ?: return null
        val address = document.roadAddressName.ifBlank { document.addressName }
        return CourseLocationSuggestion(
            id = "kakao-place-${document.id}",
            title = document.placeName,
            address = address,
            point = point,
            kind = CourseLocationKind.PLACE,
            source = CourseLocationSuggestionSource.KAKAO_KEYWORD,
            resolution = CourseLocationResolution.RESOLVED,
        )
    }

    private fun deduplicate(suggestions: List<CourseLocationSuggestion>): List<CourseLocationSuggestion> {
        val seen = HashSet<String>(suggestions.size)
        return suggestions.filter { seen.add(it.semanticKey()) }
    }

    private fun CourseLocationSuggestion.semanticKey(): String = buildString {
        append(kind.name)
        append('|')
        append(title.trim().replace(WHITESPACE, " ").lowercase())
        append('|')
        append(address.trim().replace(WHITESPACE, " ").lowercase())
        append('|')
        point?.let {
            append("%.5f,%.5f".format(java.util.Locale.US, it.lat, it.lng))
        } ?: append("unknown")
    }

    private fun KakaoAddressDocument.toPoint(): GeoPoint? = pointOrNull(x, y)

    private fun KakaoKeywordDocument.toPoint(): GeoPoint? = pointOrNull(x, y)

    private fun KakaoCoordinateAddressDocument.preferredAddress(): String? =
        roadAddress?.addressName?.takeIf { it.isNotBlank() } ?: address?.addressName?.takeIf { it.isNotBlank() }

    private fun pointOrNull(x: String, y: String): GeoPoint? = runCatching {
        GeoPoint(y.toDouble(), x.toDouble())
    }.getOrNull()

    private companion object {
        const val SERVER_PLACE_ID_PREFIX = "server-place-"
        val WHITESPACE = Regex("\\s+")
    }
}
