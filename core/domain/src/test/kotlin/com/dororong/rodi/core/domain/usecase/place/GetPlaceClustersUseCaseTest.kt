package com.dororong.rodi.core.domain.usecase.place

import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.place.PlaceException
import com.dororong.rodi.core.domain.model.place.PlaceViewportQuery
import com.dororong.rodi.core.domain.repository.PlaceRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GetPlaceClustersUseCaseTest {
    @Test
    fun `클러스터 조회와 갱신은 장소 오류를 실패 Result로 반환한다`() = runTest {
        val repository = mockk<PlaceRepository>()
        val query = query()
        val error = PlaceException.Network("연결 실패")
        coEvery { repository.getClusters(query) } throws error
        coEvery { repository.refreshClusters(query) } throws error

        assertSame(error, GetPlaceClustersUseCase(repository)(query).exceptionOrNull())
        assertSame(error, RefreshPlaceClustersUseCase(repository)(query).exceptionOrNull())
    }

    @Test
    fun `클러스터 조회와 갱신은 취소를 같은 인스턴스로 다시 던진다`() = runTest {
        val repository = mockk<PlaceRepository>()
        val query = query()
        val cancellation = CancellationException("cancelled")
        coEvery { repository.getClusters(query) } throws cancellation
        coEvery { repository.refreshClusters(query) } throws cancellation

        assertSame(cancellation, assertThrows<CancellationException> { GetPlaceClustersUseCase(repository)(query) })
        assertSame(cancellation, assertThrows<CancellationException> { RefreshPlaceClustersUseCase(repository)(query) })
    }

    private fun query() = PlaceViewportQuery(
        southWest = GeoPoint(37.4, 126.7),
        northEast = GeoPoint(37.7, 127.2),
        origin = GeoPoint(37.5, 126.9),
    )
}
