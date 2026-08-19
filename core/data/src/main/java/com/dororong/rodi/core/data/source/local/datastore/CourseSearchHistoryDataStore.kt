package com.dororong.rodi.core.data.source.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dororong.rodi.core.domain.model.course.CourseLocationKind
import com.dororong.rodi.core.domain.model.course.CourseLocationResolution
import com.dororong.rodi.core.domain.model.course.CourseLocationSuggestion
import com.dororong.rodi.core.domain.model.course.CourseLocationSuggestionSource
import com.dororong.rodi.core.domain.model.course.GeoPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private val Context.courseSearchHistoryDataStore by preferencesDataStore(name = "course_search_history")

@Singleton
class CourseSearchHistoryDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val json: Json,
) {
    fun observe(): Flow<List<CourseLocationSuggestion>> = context.courseSearchHistoryDataStore.data
        .map { preferences ->
            preferences[KEY_HISTORY]?.let(::decode).orEmpty()
                // 코스 등록 검색이 서버 연관검색 없이 카카오 로컬만 쓰도록 바뀌기 전에 저장된
                // 기록은 여전히 SERVER_REGION/SERVER_PLACE source로 남아있을 수 있다.
                .filterNot { it.source == CourseLocationSuggestionSource.SERVER_REGION || it.source == CourseLocationSuggestionSource.SERVER_PLACE }
        }
        .catch { error -> if (error is IOException) emit(emptyList()) else throw error }

    suspend fun save(suggestion: CourseLocationSuggestion) {
        requireNotNull(suggestion.point) { "검색 기록에는 좌표가 확인된 장소만 저장할 수 있습니다." }
        context.courseSearchHistoryDataStore.edit { preferences ->
            val current = preferences[KEY_HISTORY]?.let(::decode).orEmpty()
            val updated = mergeCourseSearchHistory(current, suggestion)
            preferences[KEY_HISTORY] = json.encodeToString(updated.map(CourseLocationSuggestion::toStored))
        }
    }

    suspend fun delete(id: String) {
        context.courseSearchHistoryDataStore.edit { preferences ->
            val updated = preferences[KEY_HISTORY]?.let(::decode).orEmpty().filterNot { it.id == id }
            if (updated.isEmpty()) preferences.remove(KEY_HISTORY)
            else preferences[KEY_HISTORY] = json.encodeToString(updated.map(CourseLocationSuggestion::toStored))
        }
    }

    suspend fun clear() {
        context.courseSearchHistoryDataStore.edit { it.remove(KEY_HISTORY) }
    }

    private fun decode(value: String): List<CourseLocationSuggestion> = runCatching {
        json.decodeFromString<List<StoredCourseLocation>>(value)
            .mapNotNull { it.toDomain() }
            .sortedByDescending(CourseLocationSuggestion::lastUsedAt)
    }.getOrDefault(emptyList())

    companion object {
        suspend fun clearForContext(context: Context) {
            context.courseSearchHistoryDataStore.edit { it.remove(KEY_HISTORY) }
        }

        const val MAX_HISTORY_SIZE = 15
        private val KEY_HISTORY = stringPreferencesKey("history")
    }
}

@Serializable
private data class StoredCourseLocation(
    val id: String,
    val title: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val kind: String,
    val lastUsedAt: String = "",
    val source: String = "",
    val resolution: String = "",
    val sourceId: Long? = null,
)

private fun CourseLocationSuggestion.toStored(): StoredCourseLocation {
    val resolvedPoint = requireNotNull(point)
    return StoredCourseLocation(
        id = id,
        title = title,
        address = address,
        lat = resolvedPoint.lat,
        lng = resolvedPoint.lng,
        kind = kind.name,
        lastUsedAt = lastUsedAt.toString(),
        source = source.name,
        resolution = resolution.name,
        sourceId = sourceId,
    )
}

private fun StoredCourseLocation.toDomain(): CourseLocationSuggestion? = runCatching {
    CourseLocationSuggestion(
        id = id,
        title = title,
        address = address,
        point = GeoPoint(lat, lng),
        kind = CourseLocationKind.valueOf(kind),
        lastUsedAt = lastUsedAt.takeIf(String::isNotBlank)?.let(Instant::parse) ?: Instant.EPOCH,
        source = source.toEnumOrNull<CourseLocationSuggestionSource>() ?: CourseLocationSuggestionSource.HISTORY,
        resolution = resolution.toEnumOrNull<CourseLocationResolution>() ?: CourseLocationResolution.RESOLVED,
        sourceId = sourceId,
    )
}.getOrNull()

private fun CourseLocationSuggestion.semanticKey(): String = buildString {
    append(kind.name)
    append('|')
    append(title.semanticPart())
    append('|')
    append(address.semanticPart())
    append('|')
    point?.let {
        append(it.lat.roundedCoordinate())
        append(',')
        append(it.lng.roundedCoordinate())
    } ?: append("unknown")
}

private fun String.semanticPart(): String = trim().replace(WHITESPACE, " ").lowercase()

private fun Double.roundedCoordinate(): String = "%.5f".format(java.util.Locale.US, this)

private inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? =
    takeIf(String::isNotBlank)?.let { value -> enumValues<T>().firstOrNull { it.name == value } }

private val WHITESPACE = Regex("\\s+")

internal fun mergeCourseSearchHistory(
    current: List<CourseLocationSuggestion>,
    suggestion: CourseLocationSuggestion,
    usedAt: Instant = Instant.now(),
    maxSize: Int = CourseSearchHistoryDataStore.MAX_HISTORY_SIZE,
): List<CourseLocationSuggestion> {
    requireNotNull(suggestion.point) { "검색 기록에는 좌표가 확인된 장소만 저장할 수 있습니다." }
    val updatedSuggestion = suggestion.copy(lastUsedAt = usedAt)
    val semanticKey = updatedSuggestion.semanticKey()
    return buildList {
        add(updatedSuggestion)
        addAll(current.filterNot { it.semanticKey() == semanticKey })
    }.take(maxSize)
}
