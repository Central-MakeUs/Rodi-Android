package com.dororong.rodi.core.domain.usecase.practice

import com.dororong.rodi.core.domain.model.practice.PracticeSession
import com.dororong.rodi.core.domain.repository.PracticeSessionRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PracticeSessionUseCaseTest {
    @Test
    fun `start stores a session using the injected clock`() = runTest {
        val repository = FakePracticeSessionRepository()
        val now = Instant.parse("2026-08-09T00:00:00Z")

        StartPracticeSessionUseCase(repository, Clock.fixed(now, ZoneOffset.UTC))(1L, "강남역 코스")

        assertEquals(PracticeSession(1L, "강남역 코스", now), repository.session)
    }

    @Test
    fun `session is not complete before ten minutes`() = runTest {
        val now = Instant.parse("2026-08-09T00:09:59Z")
        val repository = FakePracticeSessionRepository(
            PracticeSession(1L, "강남역 코스", now.minusSeconds(9 * 60 + 59)),
        )

        val result = GetCompletedPracticeSessionUseCase(repository, Clock.fixed(now, ZoneOffset.UTC))()

        assertNull(result.getOrThrow())
    }

    @Test
    fun `session becomes complete at ten minutes`() = runTest {
        val now = Instant.parse("2026-08-09T00:10:00Z")
        val session = PracticeSession(1L, "강남역 코스", now.minusSeconds(10 * 60))
        val repository = FakePracticeSessionRepository(session)

        val result = GetCompletedPracticeSessionUseCase(repository, Clock.fixed(now, ZoneOffset.UTC))()

        assertEquals(session, result.getOrThrow())
    }

    @Test
    fun `clear removes the stored session`() = runTest {
        val repository = FakePracticeSessionRepository(PracticeSession(1L, "강남역 코스", Instant.EPOCH))

        ClearPracticeSessionUseCase(repository)()

        assertNull(repository.session)
    }

    private class FakePracticeSessionRepository(
        var session: PracticeSession? = null,
    ) : PracticeSessionRepository {
        override suspend fun get(): PracticeSession? = session
        override suspend fun save(session: PracticeSession) {
            this.session = session
        }

        override suspend fun clear() {
            session = null
        }
    }
}
