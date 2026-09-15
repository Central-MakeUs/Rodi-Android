package com.dororong.rodi.feature.mypage

import com.dororong.rodi.core.domain.model.auth.AuthException
import com.dororong.rodi.core.domain.model.member.MyPage
import com.dororong.rodi.core.domain.model.member.LevelProgress
import com.dororong.rodi.core.domain.model.member.HardDeleteResult
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.usecase.member.GetMyPageUseCase
import com.dororong.rodi.core.domain.usecase.member.GetPracticeRecordsUseCase
import com.dororong.rodi.core.domain.usecase.member.HardDeleteAccountUseCase
import com.dororong.rodi.core.domain.model.place.CursorPage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyPageViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterEach fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `server progress percent drives the profile gauge`() = runTest(dispatcher) {
        val getMyPage = mockk<GetMyPageUseCase>()
        val getPracticeRecords = mockk<GetPracticeRecordsUseCase>()
        coEvery { getPracticeRecords(any(), any()) } returns Result.success(CursorPage(emptyList(), false, null, 0))
        coEvery { getMyPage() } returns Result.success(
            MyPage(
                nickname = "서버 닉네임",
                level = OnboardingLevel.ROOKIE,
                recommendationTags = emptyList(),
                drivingGoal = null,
                savedPlaceCount = 0,
                levelProgress = LevelProgress(
                    totalDistanceKm = 0.0,
                    currentLevelStartKm = 0.0,
                    nextLevelKm = 100.0,
                    progressPercent = 42,
                ),
            ),
        )

        val viewModel = MyPageViewModel(getMyPage, getPracticeRecords, mockk<HardDeleteAccountUseCase>())
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(0.42f, viewModel.uiState.value.profile.progress)
    }

    @Test
    fun `server my page is rendered with canonical recommendations`() = runTest(dispatcher) {
        val getMyPage = mockk<GetMyPageUseCase>()
        val getPracticeRecords = mockk<GetPracticeRecordsUseCase>()
        coEvery { getPracticeRecords(any(), any()) } returns Result.success(CursorPage(emptyList(), false, null, 0))
        coEvery { getMyPage() } returns Result.success(
            MyPage("서버 닉네임", OnboardingLevel.ROOKIE, listOf("OUTDATED"), "골목길", 7),
        )

        val viewModel = MyPageViewModel(getMyPage, getPracticeRecords, mockk<HardDeleteAccountUseCase>())
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("서버 닉네임", state.profile.nickname)
        assertEquals(listOf("유턴", "교차로", "주차"), state.profile.practiceTypes)
        assertEquals(7, state.profile.savedPlaceCount)
    }

    @Test
    fun `mypage keeps visited records and removes invisible statuses`() = runTest(dispatcher) {
        val getMyPage = mockk<GetMyPageUseCase>()
        val getPracticeRecords = mockk<GetPracticeRecordsUseCase>()
        val notVisited = com.dororong.rodi.core.domain.model.member.PracticeRecordItem(
            practiceId = 9L,
            placeId = 106L,
            placeName = "영덕 해안도로 코스",
            practiceTypes = emptyList(),
            visitCount = 0,
            visitedAt = null,
            isVerified = false,
            hasReview = true,
            status = com.dororong.rodi.core.domain.model.practice.PracticeStatus.NOT_VISITED,
        )
        val visited = notVisited.copy(
            practiceId = 10L,
            placeName = "방문한 장소",
            visitedAt = java.time.Instant.EPOCH,
            status = com.dororong.rodi.core.domain.model.practice.PracticeStatus.VISITED,
        )
        coEvery { getPracticeRecords(any(), any()) } returns Result.success(CursorPage(listOf(notVisited, visited), false, null, 2))
        coEvery { getMyPage() } returns Result.success(
            MyPage("서버 닉네임", OnboardingLevel.ROOKIE, emptyList(), null, 0),
        )

        val viewModel = MyPageViewModel(getMyPage, getPracticeRecords, mockk<HardDeleteAccountUseCase>())
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(listOf(10L), viewModel.uiState.value.practiceRecords.map { it.practiceId })
        assertEquals(
            com.dororong.rodi.core.domain.model.practice.PracticeStatus.VISITED,
            viewModel.uiState.value.practiceRecords.single().status,
        )
    }

    @Test
    fun `practice records retain the order returned by the server`() = runTest(dispatcher) {
        val getMyPage = mockk<GetMyPageUseCase>()
        val getPracticeRecords = mockk<GetPracticeRecordsUseCase>()
        val first = com.dororong.rodi.core.domain.model.member.PracticeRecordItem(
            practiceId = 2L,
            placeId = 20L,
            placeName = "두 번째 장소",
            practiceTypes = emptyList(),
            visitCount = 1,
            visitedAt = null,
            isVerified = false,
            hasReview = false,
            status = com.dororong.rodi.core.domain.model.practice.PracticeStatus.VISITED,
        )
        val second = first.copy(practiceId = 1L, placeId = 10L, placeName = "첫 번째 장소")
        coEvery { getPracticeRecords(any(), any()) } returns Result.success(CursorPage(listOf(first, second), false, null, 2))
        coEvery { getMyPage() } returns Result.success(
            MyPage("서버 닉네임", OnboardingLevel.ROOKIE, emptyList(), null, 0),
        )

        val viewModel = MyPageViewModel(getMyPage, getPracticeRecords, mockk<HardDeleteAccountUseCase>())
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(listOf(2L, 1L), viewModel.uiState.value.practiceRecords.map { it.practiceId })
    }

    @Test
    fun `practice record failure is preserved separately from profile state`() = runTest(dispatcher) {
        val getMyPage = mockk<GetMyPageUseCase>()
        val getPracticeRecords = mockk<GetPracticeRecordsUseCase>()
        coEvery { getPracticeRecords(any(), any()) } returns Result.failure(IllegalStateException("기록 조회 실패"))
        coEvery { getMyPage() } returns Result.success(
            MyPage("서버 닉네임", OnboardingLevel.ROOKIE, emptyList(), "골목길", 7),
        )

        val viewModel = MyPageViewModel(getMyPage, getPracticeRecords, mockk<HardDeleteAccountUseCase>())
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals("서버 닉네임", viewModel.uiState.value.profile.nickname)
        assertEquals("연습기록을 불러오지 못했어요.", viewModel.uiState.value.practiceRecordsErrorMessage)
    }

    @Test
    fun `refresh failure preserves loaded profile and exposes the error`() = runTest(dispatcher) {
        val getMyPage = mockk<GetMyPageUseCase>()
        val getPracticeRecords = mockk<GetPracticeRecordsUseCase>()
        coEvery { getPracticeRecords(any(), any()) } returns Result.success(CursorPage(emptyList(), false, null, 0))
        coEvery { getMyPage() } returnsMany listOf(
            Result.success(MyPage("서버 닉네임", OnboardingLevel.SEED, emptyList(), null, 3)),
            Result.failure(IllegalStateException("새로고침 실패")),
        )
        val viewModel = MyPageViewModel(getMyPage, getPracticeRecords, mockk<HardDeleteAccountUseCase>())

        viewModel.refresh()
        advanceUntilIdle()
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("서버 닉네임", state.profile.nickname)
        assertEquals("마이페이지를 불러오지 못했어요.", state.errorMessage)
    }

    @Test
    // 실제 경로에서는 리포지토리가 예외를 AuthException.Unknown으로 감싸므로 원문 차단은
    // AuthErrorMapper가 맡는다(AuthErrorMapperTest). 여기서 보는 건 그 경로를 거치지 않고
    // 올라오는 예외에 대한 이중 방어다.
    fun `non-auth failures fall back to the generic message`() = runTest(dispatcher) {
        val getMyPage = mockk<GetMyPageUseCase>()
        val getPracticeRecords = mockk<GetPracticeRecordsUseCase>()
        coEvery { getPracticeRecords(any(), any()) } returns Result.success(CursorPage(emptyList(), false, null, 0))
        coEvery { getMyPage() } returns Result.failure(
            SerializationException("Field 'nickname' is required for type with serial name 'MyPageResponse'"),
        )

        val viewModel = MyPageViewModel(getMyPage, getPracticeRecords, mockk<HardDeleteAccountUseCase>())
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals("마이페이지를 불러오지 못했어요.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `authentication failures keep their own message`() = runTest(dispatcher) {
        val getMyPage = mockk<GetMyPageUseCase>()
        val getPracticeRecords = mockk<GetPracticeRecordsUseCase>()
        coEvery { getPracticeRecords(any(), any()) } returns Result.success(CursorPage(emptyList(), false, null, 0))
        coEvery { getMyPage() } returns Result.failure(AuthException.Network("네트워크 연결을 확인해주세요."))

        val viewModel = MyPageViewModel(getMyPage, getPracticeRecords, mockk<HardDeleteAccountUseCase>())
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals("네트워크 연결을 확인해주세요.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `hard delete emits completion after the account is deleted`() = runTest(dispatcher) {
        val getMyPage = mockk<GetMyPageUseCase>()
        val getPracticeRecords = mockk<GetPracticeRecordsUseCase>()
        val hardDeleteAccount = mockk<HardDeleteAccountUseCase>()
        coEvery { hardDeleteAccount() } returns Result.success(HardDeleteResult(localCleanupSucceeded = true))
        val viewModel = MyPageViewModel(getMyPage, getPracticeRecords, hardDeleteAccount)
        val effect = async { viewModel.effect.first() }

        viewModel.hardDelete()
        assertEquals(true, viewModel.uiState.value.isHardDeleteSubmitting)
        advanceUntilIdle()

        assertEquals(MyPageEffect.HardDeleteCompleted(localCleanupSucceeded = true), effect.await())
        assertFalse(viewModel.uiState.value.isHardDeleteSubmitting)
        coVerify(exactly = 1) { hardDeleteAccount() }
    }

    @Test
    fun `hard delete exposes failure and prevents duplicate requests while submitting`() = runTest(dispatcher) {
        val getMyPage = mockk<GetMyPageUseCase>()
        val getPracticeRecords = mockk<GetPracticeRecordsUseCase>()
        val hardDeleteAccount = mockk<HardDeleteAccountUseCase>()
        coEvery { hardDeleteAccount() } returns Result.failure(
            AuthException.Network("계정 삭제 요청에 실패했어요."),
        )
        val viewModel = MyPageViewModel(getMyPage, getPracticeRecords, hardDeleteAccount)
        val effect = async { viewModel.effect.first() }

        viewModel.hardDelete()
        viewModel.hardDelete()
        advanceUntilIdle()

        assertEquals(
            MyPageEffect.ShowError("계정 삭제 요청에 실패했어요."),
            effect.await(),
        )
        assertFalse(viewModel.uiState.value.isHardDeleteSubmitting)
        coVerify(exactly = 1) { hardDeleteAccount() }
    }

    @Test
    fun `hard delete clears submitting state when cancelled`() = runTest(dispatcher) {
        val getMyPage = mockk<GetMyPageUseCase>()
        val getPracticeRecords = mockk<GetPracticeRecordsUseCase>()
        val hardDeleteAccount = mockk<HardDeleteAccountUseCase>()
        coEvery { hardDeleteAccount() } coAnswers { throw CancellationException("취소") }
        val viewModel = MyPageViewModel(getMyPage, getPracticeRecords, hardDeleteAccount)

        viewModel.hardDelete()
        try {
            advanceUntilIdle()
        } catch (_: CancellationException) {
        }

        assertFalse(viewModel.uiState.value.isHardDeleteSubmitting)
    }
}
