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
    fun `코스 등록 튜토리얼 완료는 서버에 반영한 뒤 로컬 플래그를 저장한다`() = runTest {
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
    fun `마이페이지는 비어 있을 수 있는 목표와 서버 프로필 값을 매핑한다`() = runTest {
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
    fun `빈 운전 목표는 기존 목표를 지우도록 그대로 보낸다`() = runTest {
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
    fun `필터 태그는 선택한 연습 유형을 모두 서버 값으로 보낸다`() = runTest {
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
    fun `회원 차단과 차단 해제 요청을 각각 보낸다`() = runTest {
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
    fun `자기 자신 차단의 잘못된 요청 응답을 InvalidRequest로 매핑한다`() = runTest {
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
    fun `30자를 넘는 운전 목표는 요청 전에 거부한다`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, PracticeRecordPresenceCache(), coordinator(tokenStore))

        assertThrowsSuspend<IllegalArgumentException> {
            repository.updateDrivingGoal("가".repeat(31))
        }

        coVerify(exactly = 0) { memberApi.updateMe(any()) }
    }

    @Test
    fun `탈퇴가 성공하면 연습 세션과 로그인 세션을 지운다`() = runTest {
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
    fun `세션이 없으면 탈퇴 API를 호출하지 않는다`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns null
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, PracticeRecordPresenceCache(), coordinator(tokenStore))

        val exception = assertThrowsSuspend<AuthException.NotAuthenticated> { repository.withdraw() }

        assertEquals("로그인 세션이 없습니다.", exception.message)
        coVerify(exactly = 0) { memberApi.withdraw() }
    }

    @Test
    fun `탈퇴 중 취소를 그대로 전파한다`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns AuthTokens("access", "refresh", "kakao")
        coEvery { memberApi.withdraw() } throws CancellationException("cancelled")
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, PracticeRecordPresenceCache(), coordinator(tokenStore))

        assertThrowsSuspend<CancellationException> { repository.withdraw() }
    }

    @Test
    fun `즉시 삭제가 성공하면 세션을 지우고 로컬 정리 성공을 보고한다`() = runTest {
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
    fun `서버 삭제 후 로컬 정리가 실패하면 실패를 보고한다`() = runTest {
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
    fun `코스 등록 데이터가 남으면 로컬 정리 실패를 보고한다`() = runTest {
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
    fun `세션이 없으면 즉시 삭제 API를 호출하지 않는다`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns null
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, PracticeRecordPresenceCache(), coordinator(tokenStore))

        val exception = assertThrowsSuspend<AuthException.NotAuthenticated> { repository.hardDelete() }

        assertEquals("로그인 세션이 없습니다.", exception.message)
        coVerify(exactly = 0) { memberApi.hardDelete() }
    }

    @Test
    fun `즉시 삭제 중 취소는 전파하고 로컬 데이터를 지우지 않는다`() = runTest {
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
    fun `연습 기록 여부는 성공한 첫 페이지 조회 결과를 재사용한다`() = runTest {
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
    fun `캐시가 없으면 연습 기록 여부를 한 번만 조회하고 캐시한다`() = runTest {
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
    fun `방문 예정 기록만으로는 연습 기록이 있다고 보지 않는다`() = runTest {
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
    fun `첫 페이지에 방문 기록이 없으면 다음 페이지까지 확인한다`() = runTest {
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
    fun `페이지 확인이 안전 한도에 닿으면 기록 없음을 캐시하지 않는다`() = runTest {
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
    fun `첫 페이지가 아닌 페이지에 방문이 없어도 연습 기록 있음 캐시를 덮어쓰지 않는다`() = runTest {
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
    fun `새 로그인 뒤 도착한 탈퇴 응답은 새 세션을 유지한다`() = runTest {
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
    fun `새 로그인 뒤 도착한 즉시 삭제 응답은 새 세션을 유지한다`() = runTest {
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
    fun `서버 탈퇴 성공 후 연습 정리가 실패해도 세션을 지운다`() = runTest {
        val tokenStore = realTokenStore()
        val memberApi = mockk<MemberApi>()
        coEvery { memberApi.withdraw() } returns ApiEnvelope(isSuccess = true, code = "COMMON_200", message = "성공")
        coEvery { practiceSessionRepository.clear() } throws IllegalStateException("datastore unavailable")
        val repository = MemberRepositoryImpl(memberApi, tokenStore, json, PracticeRecordPresenceCache(), coordinator(tokenStore))

        runCatching { repository.withdraw() }

        assertNull(tokenStore.getTokens())
    }

    @Test
    fun `이전 세션의 튜토리얼 완료는 새 세션에 표시하지 않는다`() = runTest {
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
