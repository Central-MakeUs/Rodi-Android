package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.cache.PracticeRecordPresenceCache
import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.local.security.AuthTokens
import com.dororong.rodi.core.data.source.remote.api.MemberApi
import com.dororong.rodi.core.data.source.remote.model.member.MemberUpdateRequest
import com.dororong.rodi.core.data.source.remote.model.member.FilterTagsRequest
import com.dororong.rodi.core.data.source.remote.model.member.CursorPagePracticeItemResponse
import com.dororong.rodi.core.data.source.remote.model.member.PracticeItemResponse
import com.dororong.rodi.core.data.source.remote.model.member.MyPageResponse
import com.dororong.rodi.core.data.source.remote.model.member.CourseTutorialCompletionResponse
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.data.test.assertThrowsSuspend
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.repository.AuthRepository
import com.dororong.rodi.core.domain.repository.PracticeSessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MemberRepositoryImplTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val practiceSessionRepository = mockk<PracticeSessionRepository>(relaxed = true)

    @Test
    fun `course tutorial completion patches server then stores local flag`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.completeCourseTutorial("Bearer access") } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = CourseTutorialCompletionResponse("2026-08-15T00:00:00Z"),
        )
        coEvery { tokenStore.markCourseTutorialCompleted() } returns true
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json, PracticeRecordPresenceCache(), practiceSessionRepository)

        repository.completeCourseTutorial()

        coVerify(exactly = 1) { memberApi.completeCourseTutorial("Bearer access") }
        coVerify(exactly = 1) { tokenStore.markCourseTutorialCompleted() }
    }

    @Test
    fun `my page maps nullable goal and server profile fields`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.getMyPage("Bearer access") } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = MyPageResponse(
                nickname = "로디",
                level = "OWNER",
                recommendationTags = listOf("SERVER_TAG"),
                drivingGoal = null,
                savedPlaceCount = 12,
            ),
        )
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json, PracticeRecordPresenceCache(), practiceSessionRepository)

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
            memberApi.updateMe("Bearer access", MemberUpdateRequest("   "))
        } returns ApiEnvelope(isSuccess = true, code = "COMMON_200", message = "성공")
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json, PracticeRecordPresenceCache(), practiceSessionRepository)

        repository.updateDrivingGoal("   ")

        coVerify { memberApi.updateMe("Bearer access", MemberUpdateRequest("   ")) }
    }

    @Test
    fun `driving goal with thirty graphemes is sent even when UTF-16 length exceeds thirty`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val goal = "가".repeat(15) + "😀".repeat(15)
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery {
            memberApi.updateMe("Bearer access", MemberUpdateRequest(goal))
        } returns ApiEnvelope(isSuccess = true, code = "COMMON_200", message = "성공")
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json, PracticeRecordPresenceCache(), practiceSessionRepository)

        repository.updateDrivingGoal(goal)

        coVerify { memberApi.updateMe("Bearer access", MemberUpdateRequest(goal)) }
    }

    @Test
    fun `filter tags send every selected practice type as wire values`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery {
            memberApi.updateFilterTags(
                "Bearer access",
                FilterTagsRequest(listOf("STRAIGHT", "PARKING", "INTERSECTION")),
            )
        } returns ApiEnvelope(isSuccess = true, code = "COMMON_200", message = "성공")
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json, PracticeRecordPresenceCache(), practiceSessionRepository)

        repository.updateFilterTags(
            listOf(PracticeType.STRAIGHT, PracticeType.PARKING, PracticeType.INTERSECTION),
        )

        coVerify {
            memberApi.updateFilterTags(
                "Bearer access",
                FilterTagsRequest(listOf("STRAIGHT", "PARKING", "INTERSECTION")),
            )
        }
    }

    @Test
    fun `block and unblock member send authenticated requests`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.blockMember("Bearer access", 7) } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = buildJsonObject { },
        )
        coEvery { memberApi.unblockMember("Bearer access", 7) } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = buildJsonObject { },
        )
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json, PracticeRecordPresenceCache(), practiceSessionRepository)

        repository.blockMember(7)
        repository.unblockMember(7)

        coVerify(exactly = 1) { memberApi.blockMember("Bearer access", 7) }
        coVerify(exactly = 1) { memberApi.unblockMember("Bearer access", 7) }
    }

    @Test
    fun `blocking self maps bad request to invalid request`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.blockMember("Bearer access", 7) } returns ApiEnvelope(
            isSuccess = false,
            code = "COMMON_400",
            message = "자기 자신은 차단할 수 없습니다.",
        )
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json, PracticeRecordPresenceCache(), practiceSessionRepository)

        assertThrowsSuspend<AuthException.InvalidRequest> { repository.blockMember(7) }
    }

    @Test
    fun `driving goal longer than thirty characters is rejected before request`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json, PracticeRecordPresenceCache(), practiceSessionRepository)

        assertThrowsSuspend<IllegalArgumentException> {
            repository.updateDrivingGoal("가".repeat(31))
        }

        coVerify(exactly = 0) { memberApi.updateMe(any(), any()) }
    }

    @Test
    fun `withdraw sends bearer access token and clears session after success`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.withdraw("Bearer access") } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
        )
        coEvery { tokenStore.clear() } returns true
        coEvery { tokenStore.clearCourseRegistrationData() } returns Unit
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json, PracticeRecordPresenceCache(), practiceSessionRepository)

        repository.withdraw()

        coVerify { memberApi.withdraw("Bearer access") }
        coVerify { practiceSessionRepository.clear() }
        coVerify { tokenStore.clear() }
    }

    @Test
    fun `withdraw does not call api without a session`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns null
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json, PracticeRecordPresenceCache(), practiceSessionRepository)

        val exception = assertThrowsSuspend<AuthException.NotAuthenticated> { repository.withdraw() }

        assertEquals("로그인 세션이 없습니다.", exception.message)
        coVerify(exactly = 0) { memberApi.withdraw(any()) }
    }

    @Test
    fun `withdraw propagates cancellation`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.withdraw("Bearer access") } throws CancellationException("cancelled")
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json, PracticeRecordPresenceCache(), practiceSessionRepository)

        assertThrowsSuspend<CancellationException> { repository.withdraw() }
    }

    @Test
    fun `hard delete sends bearer access token and clears session after success`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.hardDelete("Bearer access") } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
        )
        coEvery { tokenStore.clear() } returns true
        coEvery { tokenStore.clearCourseRegistrationData() } returns Unit
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json, PracticeRecordPresenceCache(), practiceSessionRepository)

        val result = repository.hardDelete()

        assertTrue(result.localCleanupSucceeded)
        coVerify { memberApi.hardDelete("Bearer access") }
        coVerify { practiceSessionRepository.clear() }
        coVerify { tokenStore.clear() }
    }

    @Test
    fun `hard delete reports local cleanup failure after remote deletion`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.hardDelete("Bearer access") } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
        )
        coEvery { tokenStore.clear() } returns false
        coEvery { tokenStore.clearCourseRegistrationData() } returns Unit
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json, PracticeRecordPresenceCache(), practiceSessionRepository)

        val result = repository.hardDelete()

        assertFalse(result.localCleanupSucceeded)
        coVerify { memberApi.hardDelete("Bearer access") }
        coVerify { tokenStore.clear() }
    }

    @Test
    fun `hard delete reports local cleanup failure when course registration data survives`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.hardDelete("Bearer access") } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
        )
        coEvery { tokenStore.clear() } returns true
        coEvery { tokenStore.clearCourseRegistrationData() } throws IllegalStateException("datastore unavailable")
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json, PracticeRecordPresenceCache(), practiceSessionRepository)

        val result = repository.hardDelete()

        assertFalse(result.localCleanupSucceeded)
        coVerify { tokenStore.clearCourseRegistrationData() }
    }

    @Test
    fun `hard delete does not call api without a session`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns null
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json, PracticeRecordPresenceCache(), practiceSessionRepository)

        val exception = assertThrowsSuspend<AuthException.NotAuthenticated> { repository.hardDelete() }

        assertEquals("로그인 세션이 없습니다.", exception.message)
        coVerify(exactly = 0) { memberApi.hardDelete(any()) }
    }

    @Test
    fun `hard delete propagates cancellation`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.hardDelete("Bearer access") } throws CancellationException("cancelled")
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json, PracticeRecordPresenceCache(), practiceSessionRepository)

        assertThrowsSuspend<CancellationException> { repository.hardDelete() }

        coVerify(exactly = 0) { practiceSessionRepository.clear() }
        coVerify(exactly = 0) { tokenStore.clear() }
    }

    @Test
    fun `practice presence reuses the successful first page fetch`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val cache = PracticeRecordPresenceCache()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.getPracticeRecords("Bearer access", 4, null) } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = CursorPagePracticeItemResponse(
                items = listOf(PracticeItemResponse(1, 1, "장소", status = "VISITED")),
                totalCount = 1,
            ),
        )
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json, cache, practiceSessionRepository)

        repository.getPracticeRecords(cursor = null, size = 4)

        assertEquals(true, repository.hasPracticeRecords())
        coVerify(exactly = 1) { memberApi.getPracticeRecords("Bearer access", 4, null) }
        coVerify(exactly = 0) { memberApi.getPracticeRecords("Bearer access", 1, null) }
    }

    @Test
    fun `practice presence is fetched once and then cached on a cache miss`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val cache = PracticeRecordPresenceCache()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.getPracticeRecords("Bearer access", 20, null) } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = CursorPagePracticeItemResponse(),
        )
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json, cache, practiceSessionRepository)

        assertEquals(false, repository.hasPracticeRecords())
        assertEquals(false, repository.hasPracticeRecords())

        coVerify(exactly = 1) { memberApi.getPracticeRecords("Bearer access", 20, null) }
    }

    @Test
    fun `planned records do not satisfy practice presence`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val cache = PracticeRecordPresenceCache()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.getPracticeRecords("Bearer access", 4, null) } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = CursorPagePracticeItemResponse(
                items = listOf(PracticeItemResponse(1, 1, "예정 장소", status = "PLANNED")),
                hasNext = false,
            ),
        )
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json, cache, practiceSessionRepository)

        repository.getPracticeRecords(cursor = null, size = 4)

        assertFalse(repository.hasPracticeRecords())
        coVerify(exactly = 1) { memberApi.getPracticeRecords("Bearer access", 4, null) }
        coVerify(exactly = 0) { memberApi.getPracticeRecords("Bearer access", 20, null) }
    }

    @Test
    fun `practice presence scans later pages when the first page has no visited record`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val cache = PracticeRecordPresenceCache()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.getPracticeRecords("Bearer access", 20, null) } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = CursorPagePracticeItemResponse(
                items = listOf(PracticeItemResponse(1, 1, "예정 장소", status = "PLANNED")),
                hasNext = true,
                nextCursor = "next",
            ),
        )
        coEvery { memberApi.getPracticeRecords("Bearer access", 20, "next") } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = CursorPagePracticeItemResponse(
                items = listOf(PracticeItemResponse(2, 2, "방문 장소", status = "VISITED")),
                hasNext = false,
            ),
        )
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json, cache, practiceSessionRepository)

        assertTrue(repository.hasPracticeRecords())

        coVerify(exactly = 1) { memberApi.getPracticeRecords("Bearer access", 20, null) }
        coVerify(exactly = 1) { memberApi.getPracticeRecords("Bearer access", 20, "next") }
    }

    @Test
    fun `practice presence does not cache absence when the page scan reaches its safety limit`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.getPracticeRecords("Bearer access", 20, null) } returns practicePresencePage("cursor-1")
        coEvery { memberApi.getPracticeRecords("Bearer access", 20, "cursor-1") } returns practicePresencePage("cursor-2")
        coEvery { memberApi.getPracticeRecords("Bearer access", 20, "cursor-2") } returns practicePresencePage("cursor-3")
        coEvery { memberApi.getPracticeRecords("Bearer access", 20, "cursor-3") } returns practicePresencePage("cursor-4")
        coEvery { memberApi.getPracticeRecords("Bearer access", 20, "cursor-4") } returns practicePresencePage("cursor-5")
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json, PracticeRecordPresenceCache(), practiceSessionRepository)

        assertFalse(repository.hasPracticeRecords())
        assertFalse(repository.hasPracticeRecords())

        coVerify(exactly = 2) { memberApi.getPracticeRecords("Bearer access", 20, null) }
        coVerify(exactly = 2) { memberApi.getPracticeRecords("Bearer access", 20, "cursor-4") }
    }

    @Test
    fun `a non-initial page without visits does not overwrite a true presence cache`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val cache = PracticeRecordPresenceCache().apply { set(true) }
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.getPracticeRecords("Bearer access", 4, "last") } returns ApiEnvelope(
            isSuccess = true,
            code = "COMMON_200",
            message = "성공",
            data = CursorPagePracticeItemResponse(
                items = listOf(PracticeItemResponse(3, 3, "예정 장소", status = "PLANNED")),
                hasNext = false,
            ),
        )
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json, cache, practiceSessionRepository)

        repository.getPracticeRecords(cursor = "last", size = 4)

        assertTrue(repository.hasPracticeRecords())
        coVerify(exactly = 1) { memberApi.getPracticeRecords("Bearer access", 4, "last") }
        coVerify(exactly = 0) { memberApi.getPracticeRecords("Bearer access", 20, null) }
    }

    private fun practicePresencePage(nextCursor: String) = ApiEnvelope(
        isSuccess = true,
        code = "COMMON_200",
        message = "성공",
        data = CursorPagePracticeItemResponse(
            items = listOf(PracticeItemResponse(1, 1, "예정 장소", status = "PLANNED")),
            hasNext = true,
            nextCursor = nextCursor,
        ),
    )
}
