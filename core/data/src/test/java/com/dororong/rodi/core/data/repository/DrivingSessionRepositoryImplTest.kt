package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.datastore.DrivingSessionPreferences
import com.dororong.rodi.core.domain.model.course.GeoPoint
import com.dororong.rodi.core.domain.model.driving.DrivingSession
import com.dororong.rodi.core.domain.model.driving.DrivingSessionStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DrivingSessionRepositoryImplTest {
    private val preferences = mockk<DrivingSessionPreferences>()
    private val session = DrivingSession(
        id = "session",
        placeId = 1L,
        placeName = "연습 코스",
        destination = GeoPoint(37.5, 127.0),
        plannedDistanceMeters = 1_000,
        startedAtEpochMillis = 1L,
        arrivedAtEpochMillis = null,
        traveledDistanceMeters = 0.0,
        status = DrivingSessionStatus.ACTIVE,
        isArrivalNoticePending = false,
    )

    @Test
    fun `exposes stored session and delegates state transitions`() = runTest {
        every { preferences.session } returns flowOf(session)
        coEvery { preferences.start(session) } returns Unit
        coEvery { preferences.updateProgress(session.id, 120.0) } returns Unit
        coEvery { preferences.markArrived(session.id, 2L, 120.0) } returns true
        coEvery { preferences.acknowledgeArrival(session.id) } returns Unit
        coEvery { preferences.clear(session.id) } returns Unit
        val repository = DrivingSessionRepositoryImpl(preferences)

        assertEquals(session, repository.session.first())
        repository.start(session)
        repository.updateProgress(session.id, 120.0)
        val arrived = repository.markArrived(session.id, 2L, 120.0)
        repository.acknowledgeArrival(session.id)
        repository.clear(session.id)

        assertTrue(arrived)
        coVerify(exactly = 1) { preferences.start(session) }
        coVerify(exactly = 1) { preferences.updateProgress(session.id, 120.0) }
        coVerify(exactly = 1) { preferences.markArrived(session.id, 2L, 120.0) }
        coVerify(exactly = 1) { preferences.acknowledgeArrival(session.id) }
        coVerify(exactly = 1) { preferences.clear(session.id) }
    }
}
