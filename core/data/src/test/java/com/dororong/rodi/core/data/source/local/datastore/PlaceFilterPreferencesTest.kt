package com.dororong.rodi.core.data.source.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.dororong.rodi.core.domain.model.place.PracticeType
import java.nio.file.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class PlaceFilterPreferencesTest {
    @TempDir
    lateinit var directory: Path

    private val scopes = mutableListOf<CoroutineScope>()

    @AfterEach
    fun tearDown() = runBlocking {
        scopes.forEach { it.coroutineContext.job.cancelAndJoin() }
    }

    @Test
    fun `저장한 필터가 없으면 모든 장소를 조회하는 빈 집합을 반환한다`() = runTest {
        assertEquals(emptySet<PracticeType>(), PlaceFilterPreferences(store()).getPracticeTypes())
    }

    @Test
    fun `선택한 필터는 DataStore 인스턴스를 재생성해도 복원한다`() = runTest {
        val selected = setOf(PracticeType.PARKING, PracticeType.LEFT_RIGHT_TURN)
        val firstStore = store()
        val preferences = PlaceFilterPreferences(firstStore)
        preferences.setPracticeTypes(selected)
        assertEquals(selected, preferences.getPracticeTypes())
        scopes.last().coroutineContext.job.cancelAndJoin()

        val restored = PlaceFilterPreferences(store()).getPracticeTypes()

        assertEquals(selected, restored)
    }

    @Test
    fun `필터를 빈 집합으로 해제하면 재생성 뒤에도 선택이 남지 않는다`() = runTest {
        val preferences = PlaceFilterPreferences(store())
        preferences.setPracticeTypes(setOf(PracticeType.STRAIGHT))
        preferences.setPracticeTypes(emptySet())
        assertEquals(emptySet<PracticeType>(), preferences.getPracticeTypes())
        scopes.last().coroutineContext.job.cancelAndJoin()

        assertEquals(emptySet<PracticeType>(), PlaceFilterPreferences(store()).getPracticeTypes())
    }

    @Test
    fun `장소 필터는 정렬 태그를 초기 선택으로 읽거나 덮어쓰지 않는다`() = runTest {
        val dataStore = store()
        val sortTagsKey = stringSetPreferencesKey("sort_tags")
        val filterKey = stringSetPreferencesKey("practice_types")
        dataStore.edit { it[sortTagsKey] = setOf("U_TURN", "STRAIGHT") }
        val preferences = PlaceFilterPreferences(dataStore)
        assertEquals(emptySet<PracticeType>(), preferences.getPracticeTypes())

        preferences.setPracticeTypes(setOf(PracticeType.PARKING))

        val stored = dataStore.data.first()
        assertEquals(setOf("U_TURN", "STRAIGHT"), stored[sortTagsKey])
        assertEquals(setOf("PARKING"), stored[filterKey])
        assertEquals(setOf(PracticeType.PARKING), preferences.getPracticeTypes())
    }

    private fun store(): DataStore<Preferences> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scopes += scope
        return PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { directory.resolve("place_filters.preferences_pb").toFile() },
        )
    }
}
