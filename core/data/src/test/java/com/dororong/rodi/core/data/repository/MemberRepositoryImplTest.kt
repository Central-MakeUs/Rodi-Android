package com.dororong.rodi.core.data.repository

import com.dororong.rodi.core.data.source.local.security.AuthTokenStore
import com.dororong.rodi.core.data.source.local.security.AuthTokens
import com.dororong.rodi.core.data.source.remote.api.MemberApi
import com.dororong.rodi.core.data.source.remote.model.member.MemberUpdateRequest
import com.dororong.rodi.core.data.source.remote.model.member.FilterTagsRequest
import com.dororong.rodi.core.data.source.remote.model.member.MyPageResponse
import com.dororong.rodi.core.data.source.remote.network.ApiEnvelope
import com.dororong.rodi.core.data.test.assertThrowsSuspend
import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.model.place.PracticeType
import com.dororong.rodi.core.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MemberRepositoryImplTest {
    private val json = Json { ignoreUnknownKeys = true }

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
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json)

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
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json)

        repository.updateDrivingGoal("   ")

        coVerify { memberApi.updateMe("Bearer access", MemberUpdateRequest("   ")) }
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
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json)

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
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json)

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
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json)

        assertThrowsSuspend<AuthException.InvalidRequest> { repository.blockMember(7) }
    }

    @Test
    fun `driving goal longer than thirty characters is rejected before request`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json)

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
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json)

        repository.withdraw()

        coVerify { memberApi.withdraw("Bearer access") }
        coVerify { tokenStore.clear() }
    }

    @Test
    fun `withdraw does not call api without a session`() = runTest {
        val memberApi = mockk<MemberApi>()
        val tokenStore = mockk<AuthTokenStore>()
        coEvery { tokenStore.getTokens() } returns null
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json)

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
        val repository = MemberRepositoryImpl(memberApi, tokenStore, mockk<AuthRepository>(), json)

        assertThrowsSuspend<CancellationException> { repository.withdraw() }
    }
}
