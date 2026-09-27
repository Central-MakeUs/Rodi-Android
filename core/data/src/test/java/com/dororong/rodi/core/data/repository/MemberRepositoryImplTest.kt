package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.cache.PracticeRecordPresenceCache
import android.content.Context
import com.dororong.rodi.core.data.source.local.datastore.AuthTokenDataStore
import com.dororong.rodi.core.data.source.local.security.AuthTokenMutationResult
import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.local.security.AuthTokens
import com.dororong.rodi.core.data.source.remote.api.MemberApi
import com.dororong.rodi.core.data.source.remote.model.member.MemberUpdateRequest
import com.dororong.rodi.core.data.source.remote.model.member.FilterTagsRequest
import com.dororong.rodi.core.data.source.remote.model.member.CursorPagePracticeItemResponse
import com.dororong.rodi.core.data.source.remote.model.member.PracticeItemResponse
import com.dororong.rodi.core.data.source.remote.model.member.LevelProgressResponse
import com.dororong.rodi.core.data.source.remote.model.member.MyPageResponse
import com.dororong.rodi.core.data.source.remote.model.member.CourseTutorialCompletionResponse
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.data.test.assertThrowsSuspend
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.repository.AuthRepository
import com.dororong.rodi.core.domain.repository.EntryRepository
import com.dororong.rodi.core.domain.repository.OnboardingRepository
import com.dororong.rodi.core.domain.repository.PracticeSessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MemberRepositoryImplTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val practiceSessionRepository = mockk<PracticeSessionRepository>(relaxed = true)
    private val onboardingRepository = mockk<OnboardingRepository>(relaxed = true)
    private val entryRepository = mockk<EntryRepository>(relaxed = true)

    private fun coordinator(
        tokenStore: AuthTokenStore,
        cache: PracticeRecordPresenceCache = PracticeRecordPresenceCache(),
    ) = AuthSessionCoordinator(tokenStore, practiceSessionRepository, cache, onboardingRepository, entryRepository)

    @Test
    fun `course tutorial completion patches server then stores local flag`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.completeCourseTutorial() } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = CourseTutorialCompletionResponse("2026-08-15T00:00:00Z"),
        )
        coEvery { tokenStore.markCourseTutorialCompleted(any()) } returns true
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, PracticeRecordPresenceCache(), coordinator(tokenStore))

        repository.completeCourseTutorial()

        coVerify(exactly = 1) { memberApi.completeCourseTutorial() }
        coVerify(exactly = 1) { tokenStore.markCourseTutorialCompleted(any()) }
    }

    @Test
    fun `my page maps nullable goal and server profile fields`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.getMyPage() } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = MyPageResponse(
                nickname = "로디",
                level = "OWNER",
                recommendationTags = listOf("SERVER_TAG"),
                drivingGoal = null,
                savedPlaceCount = 12,
                levelProgress = LevelProgressResponse(totalDistanceKm = 0.0, currentLevelStartKm = 0.0, progressPercent = 0),
            ),
        )
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, PracticeRecordPresenceCache(), coordinator(tokenStore))

        val result = repository.getMyPage()

        assertEquals("로디", result.nickname)
        assertEquals(null, result.drivingGoal)
        assertEquals(12, result.savedPlaceCount)
    }

    @Test
    fun `blank driving goal is sent to delete the existing goal`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery {
            memberApi.updateMe(MemberUpdateRequest("   "))
        } returns ApiEnvelope(isSuccess = true, code = "COMMON_200", message = "성공")
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, PracticeRecordPresenceCache(), coordinator(tokenStore))

        repository.updateDrivingGoal("   ")

        coVerify { memberApi.updateMe(MemberUpdateRequest("   ")) }
    }

    @Test
    fun `filter tags send every selected practice type as wire values`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery {
            memberApi.updateFilterTags(
                FilterTagsRequest(listOf("STRAIGHT", "PARKING", "INTERSECTION")),
            )
        } returns ApiEnvelope(isSuccess = true, code = "COMMON_200", message = "성공")
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, PracticeRecordPresenceCache(), coordinator(tokenStore))

        repository.updateFilterTags(
            listOf(PracticeType.STRAIGHT, PracticeType.PARKING, PracticeType.INTERSECTION),
        )

        coVerify {
            memberApi.updateFilterTags(
                FilterTagsRequest(listOf("STRAIGHT", "PARKING", "INTERSECTION")),
            )
        }
    }

    @Test
    fun `block and unblock member send authenticated requests`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.blockMember(7) } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = buildJsonObject { },
        )
        coEvery { memberApi.unblockMember(7) } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = buildJsonObject { },
        )
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, PracticeRecordPresenceCache(), coordinator(tokenStore))

        repository.blockMember(7)
        repository.unblockMember(7)

        coVerify(exactly = 1) { memberApi.blockMember(7) }
        coVerify(exactly = 1) { memberApi.unblockMember(7) }
    }

    @Test
    fun `blocking self maps bad request to invalid request`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.blockMember(7) } returns ApiEnvelope(
            isSuccess = false,
            code = "COMMON_400",
            message = "자기 자신은 차단할 수 없습니다.",
        )
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, PracticeRecordPresenceCache(), coordinator(tokenStore))

        assertThrowsSuspend<AuthException.InvalidRequest> { repository.blockMember(7) }
    }

    @Test
    fun `driving goal longer than thirty characters is rejected before request`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, PracticeRecordPresenceCache(), coordinator(tokenStore))

        assertThrowsSuspend<IllegalArgumentException> {
            repository.updateDrivingGoal("가".repeat(31))
        }

        coVerify(exactly = 0) { memberApi.updateMe(any()) }
    }

    @Test
    fun `withdraw sends bearer access token and clears session after success`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.withdraw() } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
        )
        coEvery { tokenStore.clearSession(any()) } returns AuthTokenMutationResult.APPLIED
        coEvery { tokenStore.clearCourseRegistrationData() } returns Unit
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, PracticeRecordPresenceCache(), coordinator(tokenStore))

        repository.withdraw()

        coVerify { memberApi.withdraw() }
        coVerify { practiceSessionRepository.clear() }
        coVerify { tokenStore.clearSession(any()) }
    }

    @Test
    fun `withdraw does not call api without a session`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns null
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, PracticeRecordPresenceCache(), coordinator(tokenStore))

        val exception = assertThrowsSuspend<AuthException.NotAuthenticated> { repository.withdraw() }

        assertEquals("로그인 세션이 없습니다.", exception.message)
        coVerify(exactly = 0) { memberApi.withdraw() }
    }

    @Test
    fun `withdraw propagates cancellation`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.withdraw() } throws CancellationException("cancelled")
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, PracticeRecordPresenceCache(), coordinator(tokenStore))

        assertThrowsSuspend<CancellationException> { repository.withdraw() }
    }

    @Test
    fun `hard delete sends bearer access token and clears session after success`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.hardDelete() } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
        )
        coEvery { tokenStore.clearSession(any()) } returns AuthTokenMutationResult.APPLIED
        coEvery { tokenStore.clearCourseRegistrationData() } returns Unit
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, PracticeRecordPresenceCache(), coordinator(tokenStore))

        val result = repository.hardDelete()

        assertTrue(result.localCleanupSucceeded)
        coVerify { memberApi.hardDelete() }
        coVerify { practiceSessionRepository.clear() }
        coVerify { tokenStore.clearSession(any()) }
    }

    @Test
    fun `hard delete reports local cleanup failure after remote deletion`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.hardDelete() } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
        )
        coEvery { tokenStore.clearSession(any()) } returns AuthTokenMutationResult.FAILED
        coEvery { tokenStore.clearCourseRegistrationData() } returns Unit
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, PracticeRecordPresenceCache(), coordinator(tokenStore))

        val result = repository.hardDelete()

        assertFalse(result.localCleanupSucceeded)
        coVerify { memberApi.hardDelete() }
        coVerify { tokenStore.clearSession(any()) }
    }

    @Test
    fun `hard delete reports local cleanup failure when course registration data survives`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.hardDelete() } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
        )
        coEvery { tokenStore.clearSession(any()) } returns AuthTokenMutationResult.APPLIED
        coEvery { tokenStore.clearCourseRegistrationData() } throws IllegalStateException("datastore unavailable")
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, PracticeRecordPresenceCache(), coordinator(tokenStore))

        val result = repository.hardDelete()

        assertFalse(result.localCleanupSucceeded)
        coVerify { tokenStore.clearCourseRegistrationData() }
    }

    @Test
    fun `hard delete does not call api without a session`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns null
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, PracticeRecordPresenceCache(), coordinator(tokenStore))

        val exception = assertThrowsSuspend<AuthException.NotAuthenticated> { repository.hardDelete() }

        assertEquals("로그인 세션이 없습니다.", exception.message)
        coVerify(exactly = 0) { memberApi.hardDelete() }
    }

    @Test
    fun `hard delete propagates cancellation`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.hardDelete() } throws CancellationException("cancelled")
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, PracticeRecordPresenceCache(), coordinator(tokenStore))

        assertThrowsSuspend<CancellationException> { repository.hardDelete() }

        coVerify(exactly = 0) { practiceSessionRepository.clear() }
        coVerify(exactly = 0) { tokenStore.clearSession(any()) }
    }

    @Test
    fun `practice presence reuses the successful first page fetch`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val cache = PracticeRecordPresenceCache()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.getPracticeRecords(4, null) } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = CursorPagePracticeItemResponse(
                items = listOf(PracticeItemResponse(1, 1, "장소", practiceTypes = emptyList(), status = "VISITED", visitCount = 0, hasReview = false)),
                hasNext = false,
                totalCount = 1,
            ),
        )
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, cache, coordinator(tokenStore))

        repository.getPracticeRecords(cursor = null, size = 4)

        assertEquals(true, repository.hasPracticeRecords())
        coVerify(exactly = 1) { memberApi.getPracticeRecords(4, null) }
        coVerify(exactly = 0) { memberApi.getPracticeRecords(1, null) }
    }

    @Test
    fun `practice presence is fetched once and then cached on a cache miss`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val cache = PracticeRecordPresenceCache()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.getPracticeRecords(20, null) } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = CursorPagePracticeItemResponse(items = emptyList(), hasNext = false),
        )
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, cache, coordinator(tokenStore))

        assertEquals(false, repository.hasPracticeRecords())
        assertEquals(false, repository.hasPracticeRecords())

        coVerify(exactly = 1) { memberApi.getPracticeRecords(20, null) }
    }

    @Test
    fun `planned records do not satisfy practice presence`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val cache = PracticeRecordPresenceCache()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.getPracticeRecords(4, null) } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = CursorPagePracticeItemResponse(
                items = listOf(PracticeItemResponse(1, 1, "예정 장소", practiceTypes = emptyList(), status = "PLANNED", visitCount = 0, hasReview = false)),
                hasNext = false,
            ),
        )
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, cache, coordinator(tokenStore))

        repository.getPracticeRecords(cursor = null, size = 4)

        assertFalse(repository.hasPracticeRecords())
        coVerify(exactly = 1) { memberApi.getPracticeRecords(4, null) }
        coVerify(exactly = 0) { memberApi.getPracticeRecords(20, null) }
    }

    @Test
    fun `practice presence scans later pages when the first page has no visited record`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val cache = PracticeRecordPresenceCache()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.getPracticeRecords(20, null) } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = CursorPagePracticeItemResponse(
                items = listOf(PracticeItemResponse(1, 1, "예정 장소", practiceTypes = emptyList(), status = "PLANNED", visitCount = 0, hasReview = false)),
                hasNext = true,
                nextCursor = "next",
            ),
        )
        coEvery { memberApi.getPracticeRecords(20, "next") } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = CursorPagePracticeItemResponse(
                items = listOf(PracticeItemResponse(2, 2, "방문 장소", practiceTypes = emptyList(), status = "VISITED", visitCount = 0, hasReview = false)),
                hasNext = false,
            ),
        )
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, cache, coordinator(tokenStore))

        assertTrue(repository.hasPracticeRecords())

        coVerify(exactly = 1) { memberApi.getPracticeRecords(20, null) }
        coVerify(exactly = 1) { memberApi.getPracticeRecords(20, "next") }
    }

    @Test
    fun `practice presence does not cache absence when the page scan reaches its safety limit`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.getPracticeRecords(20, null) } returns practicePresencePage("cursor-1")
        coEvery { memberApi.getPracticeRecords(20, "cursor-1") } returns practicePresencePage("cursor-2")
        coEvery { memberApi.getPracticeRecords(20, "cursor-2") } returns practicePresencePage("cursor-3")
        coEvery { memberApi.getPracticeRecords(20, "cursor-3") } returns practicePresencePage("cursor-4")
        coEvery { memberApi.getPracticeRecords(20, "cursor-4") } returns practicePresencePage("cursor-5")
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, PracticeRecordPresenceCache(), coordinator(tokenStore))

        assertFalse(repository.hasPracticeRecords())
        assertFalse(repository.hasPracticeRecords())

        coVerify(exactly = 2) { memberApi.getPracticeRecords(20, null) }
        coVerify(exactly = 2) { memberApi.getPracticeRecords(20, "cursor-4") }
    }

    @Test
    fun `a non-initial page without visits does not overwrite a true presence cache`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val cache = PracticeRecordPresenceCache().apply { set(true) }
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.getPracticeRecords(4, "last") } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = CursorPagePracticeItemResponse(
                items = listOf(PracticeItemResponse(3, 3, "예정 장소", practiceTypes = emptyList(), status = "PLANNED", visitCount = 0, hasReview = false)),
                hasNext = false,
            ),
        )
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, cache, coordinator(tokenStore))

        repository.getPracticeRecords(cursor = "last", size = 4)

        assertTrue(repository.hasPracticeRecords())
        coVerify(exactly = 1) { memberApi.getPracticeRecords(4, "last") }
        coVerify(exactly = 0) { memberApi.getPracticeRecords(20, null) }
    }

    @Test
    fun `withdraw response after a new login keeps the new session`() = runTest {
        val tokenStore = realTokenStore()
        val memberApi = mockk<MemberApi>()
        val started = CompletableDeferred<Unit>()
        val response = CompletableDeferred<ApiEnvelope<JsonObject>>()
        coEvery { memberApi.withdraw() } coAnswers {
            started.complete(Unit)
            response.await()
        }
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, PracticeRecordPresenceCache(), coordinator(tokenStore))
        val withdraw = async { runCatching { repository.withdraw() } }
        started.await()

        tokenStore.save("access-b", "refresh-b")
        response.complete(ApiEnvelope(isSuccess = true, code = "COMMON_200", message = "성공"))
        val result = withdraw.await()

        assertTrue(result.exceptionOrNull() is AuthException.NotAuthenticated)
        assertEquals("access-b", tokenStore.getTokens()?.accessToken)
        coVerify(exactly = 0) { practiceSessionRepository.clear() }
    }

    @Test
    fun `hard delete response after a new login keeps the new session`() = runTest {
        val tokenStore = realTokenStore()
        val memberApi = mockk<MemberApi>()
        val started = CompletableDeferred<Unit>()
        val response = CompletableDeferred<ApiEnvelope<JsonObject>>()
        coEvery { memberApi.hardDelete() } coAnswers {
            started.complete(Unit)
            response.await()
        }
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, PracticeRecordPresenceCache(), coordinator(tokenStore))
        val hardDelete = async { runCatching { repository.hardDelete() } }
        started.await()

        tokenStore.save("access-b", "refresh-b")
        response.complete(ApiEnvelope(isSuccess = true, code = "COMMON_200", message = "성공"))
        val result = hardDelete.await()

        assertTrue(result.exceptionOrNull() is AuthException.NotAuthenticated)
        assertEquals("access-b", tokenStore.getTokens()?.accessToken)
        coVerify(exactly = 0) { practiceSessionRepository.clear() }
    }

    @Test
    fun `withdraw clears the session even when practice cleanup fails after server success`() = runTest {
        val tokenStore = realTokenStore()
        val memberApi = mockk<MemberApi>()
        coEvery { memberApi.withdraw() } returns ApiEnvelope(isSuccess = true, code = "COMMON_200", message = "성공")
        coEvery { practiceSessionRepository.clear() } throws IllegalStateException("datastore unavailable")
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, PracticeRecordPresenceCache(), coordinator(tokenStore))

        runCatching { repository.withdraw() }

        assertNull(tokenStore.getTokens())
    }

    @Test
    fun `tutorial completion from an old session does not mark a new session`() = runTest {
        val tokenStore = realTokenStore()
        val memberApi = mockk<MemberApi>()
        val started = CompletableDeferred<Unit>()
        val response = CompletableDeferred<ApiEnvelope<CourseTutorialCompletionResponse>>()
        coEvery { memberApi.completeCourseTutorial() } coAnswers {
            started.complete(Unit)
            response.await()
        }
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, PracticeRecordPresenceCache(), coordinator(tokenStore))
        val completion = async { runCatching { repository.completeCourseTutorial() } }
        started.await()

        tokenStore.save("access-b", "refresh-b")
        response.complete(
            ApiEnvelope(
                isSuccess = true,
                code = "COMMON_200",
                message = "성공",
                data = CourseTutorialCompletionResponse("2026-08-15T00:00:00Z"),
            ),
        )
        completion.await()

        assertFalse(tokenStore.getTokens()?.isCourseTutorialCompleted == true)
    }

    private fun realTokenStore(): AuthTokenStore {
        val context = mockk<Context>()
        val dataStore = mockk<AuthTokenDataStore>()
        every { context.deleteSharedPreferences(any()) } returns true
        coEvery { dataStore.read() } returns AuthTokens("access-a", "refresh-a", "kakao")
        coEvery { dataStore.save(any()) } returns true
        coEvery { dataStore.clear(any()) } returns true
        return spyk(AuthTokenStore(context, dataStore)).also {
            coEvery { it.clearCourseRegistrationData() } returns Unit
        }
    }

    private fun practicePresencePage(nextCursor: String) = ApiEnvelope(
        isSuccess = true,
        code = "COMMON_200",
        message = "성공",
        data = CursorPagePracticeItemResponse(
            items = listOf(PracticeItemResponse(1, 1, "예정 장소", practiceTypes = emptyList(), status = "PLANNED", visitCount = 0, hasReview = false)),
            hasNext = true,
            nextCursor = nextCursor,
        ),
    )
}
