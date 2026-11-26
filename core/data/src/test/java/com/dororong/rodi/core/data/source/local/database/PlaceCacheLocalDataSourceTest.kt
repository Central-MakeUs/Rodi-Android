package com.dororong.rodi.core.data.source.local.database

import com.dororong.rodi.core.data.source.local.database.dao.PlaceCacheDao
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceSummary
import com.dororong.rodi.core.domain.model.place.PlaceType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class PlaceCacheLocalDataSourceTest {
    @Test
    fun `삭제를 관측한 코스는 요약과 좌표 캐시에서 함께 제거하고 활성 장소만 저장한다`() = runTest {
        val dao = mockk<PlaceCacheDao>()
        coEvery { dao.upsertSummariesWithCoordinates(any(), any(), any()) } returns Unit
        val active = summary(1)
        val deleted = summary(2).copy(isDeleted = true)

        PlaceCacheLocalDataSource(dao).upsertSummaries(listOf(active, deleted))

        coVerify(exactly = 1) {
            dao.upsertSummariesWithCoordinates(
                summaries = match { it.map { item -> item.id } == listOf(1L) },
                coordinates = match { it.map { item -> item.id } == listOf(1L) },
                deletedIds = listOf(2L),
            )
        }
    }

    private fun summary(id: Long) = PlaceSummary(
        id = id,
        type = PlaceType.COURSE,
        name = "코스 $id",
        address = "서울",
        point = GeoPoint(37.5, 126.9),
        distanceFromMeMeters = null,
        practiceTypes = emptyList(),
        description = null,
        distanceMeters = null,
        capacity = null,
        openTime = null,
        isDeleted = false,
    )
}
