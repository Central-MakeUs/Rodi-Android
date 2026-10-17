package com.dororong.rodi.feature.mypage.nickname

import app.cash.turbine.test
import com.dororong.rodi.core.domain.model.member.MyPage
import com.dororong.rodi.core.domain.model.member.NicknameAvailability
import com.dororong.rodi.core.domain.model.member.NicknameRejectedException
import com.dororong.rodi.core.domain.model.member.NicknameRejection
import com.dororong.rodi.core.domain.model.onboarding.OnboardingLevel
import com.dororong.rodi.core.domain.usecase.member.CheckNicknameAvailabilityUseCase
import com.dororong.rodi.core.domain.usecase.member.GetMyPageUseCase
import com.dororong.rodi.core.domain.usecase.member.UpdateNicknameUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NicknameViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val getMyPage = mockk<GetMyPageUseCase>()
    private val check = mockk<CheckNicknameAvailabilityUseCase>()
    private val update = mockk<UpdateNicknameUseCase>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { getMyPage() } returns Result.success(MyPage("기존_닉네임", OnboardingLevel.SEED, emptyList(), null, 0))
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `프로필 로드가 실패하면 편집과 저장을 막고 재시도로 복구한다`() = runTest(dispatcher) {
        coEvery { getMyPage() } returns Result.failure(IllegalStateException("failed"))
        val vm = NicknameViewModel(getMyPage, check, update)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.loadFailed)
        vm.changeNickname("수정")
        vm.validate()
        vm.save()
        coVerify(exactly = 0) { check(any()) }
        coVerify(exactly = 0) { update(any()) }
        assertEquals("", vm.uiState.value.nickname)
        coEvery { getMyPage() } returns Result.success(MyPage("복구_닉네임", OnboardingLevel.SEED, emptyList(), null, 0))
        vm.load()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.loadFailed)
        assertEquals("복구_닉네임", vm.uiState.value.nickname)
    }

    @Test
    fun `입력할 때 검증하지 않고 명시적으로 검증한 뒤에만 저장할 수 있다`() = runTest(dispatcher) {
        coEvery { check("새_닉네임") } returns Result.success(NicknameAvailability("새_닉네임", true, null))
        val vm = NicknameViewModel(getMyPage, check, update)
        advanceUntilIdle()
        assertEquals("기존_닉네임", vm.uiState.value.nickname)
        assertFalse(vm.uiState.value.canSave)
        vm.changeNickname("새_닉네임")
        advanceUntilIdle()
        coVerify(exactly = 0) { check(any()) }
        vm.save()
        coVerify(exactly = 0) { update(any()) }
        vm.validate()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.canSave)
        vm.changeNickname("다른_닉네임")
        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun `동일 입력에 대한 중복 검증을 보내지 않는다`() = runTest(dispatcher) {
        val response = CompletableDeferred<Result<NicknameAvailability>>()
        coEvery { check(any()) } coAnswers { response.await() }
        val vm = NicknameViewModel(getMyPage, check, update)
        advanceUntilIdle()
        vm.changeNickname("새_닉네임")
        vm.validate()
        vm.validate()
        runCurrent()
        coVerify(exactly = 1) { check("새_닉네임") }
        response.complete(Result.success(NicknameAvailability("새_닉네임", true, null)))
        advanceUntilIdle()
        vm.validate()
        coVerify(exactly = 1) { check(any()) }
    }

    @Test
    fun `이전 입력의 늦은 검증 응답은 새 입력을 사용 가능으로 바꾸지 않는다`() = runTest(dispatcher) {
        val response = CompletableDeferred<Result<NicknameAvailability>>()
        coEvery { check("첫_닉네임") } coAnswers { withContext(NonCancellable) { response.await() } }
        val vm = NicknameViewModel(getMyPage, check, update)
        advanceUntilIdle()
        vm.changeNickname("첫_닉네임")
        vm.validate()
        runCurrent()
        vm.changeNickname("새_닉네임")
        response.complete(Result.success(NicknameAvailability("첫_닉네임", true, null)))
        advanceUntilIdle()
        assertEquals("새_닉네임", vm.uiState.value.nickname)
        assertNull(vm.uiState.value.validatedNickname)
        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun `저장에는 서버 정규화 값을 쓰고 중복 저장과 저장 중 편집을 막는다`() = runTest(dispatcher) {
        val response = CompletableDeferred<Result<Unit>>()
        coEvery { check(" 새닉네임 ") } returns Result.success(NicknameAvailability("새닉네임", true, null))
        coEvery { update("새닉네임") } coAnswers { response.await() }
        val vm = NicknameViewModel(getMyPage, check, update)
        advanceUntilIdle()
        vm.changeNickname(" 새닉네임 ")
        vm.validate()
        advanceUntilIdle()
        vm.save()
        vm.save()
        vm.changeNickname("수정")
        runCurrent()
        assertEquals(" 새닉네임 ", vm.uiState.value.nickname)
        assertTrue(vm.uiState.value.isSaving)
        coVerify(exactly = 1) { update("새닉네임") }
        response.complete(Result.success(Unit))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.saveSucceeded)
        vm.save()
        coVerify(exactly = 1) { update(any()) }
    }

    @Test
    fun `저장 실패 시 입력과 검증 결과를 유지하고 오류를 알리며 재시도할 수 있다`() = runTest(dispatcher) {
        coEvery { check(any()) } returns Result.success(NicknameAvailability("새_닉네임", true, null))
        coEvery { update(any()) } returns Result.failure(IllegalStateException("private failure"))
        val vm = NicknameViewModel(getMyPage, check, update)
        advanceUntilIdle()
        vm.changeNickname("새_닉네임")
        vm.validate()
        advanceUntilIdle()
        vm.effect.test {
            vm.save()
            advanceUntilIdle()
            assertEquals(NicknameEffect.SaveFailed, awaitItem())
        }
        assertEquals("새_닉네임", vm.uiState.value.nickname)
        assertFalse(vm.uiState.value.saveSucceeded)
        assertTrue(vm.uiState.value.canSave)
        coEvery { update(any()) } returns Result.success(Unit)
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.saveSucceeded)
        coVerify(exactly = 2) { update("새_닉네임") }
    }

    @Test
    fun `검증 후 닉네임이 선점되면 저장 응답의 중복 사유로 안내한다`() = runTest(dispatcher) {
        coEvery { check(any()) } returns Result.success(NicknameAvailability("새_닉네임", true, null))
        coEvery { update(any()) } returns Result.failure(NicknameRejectedException(NicknameRejection.DUPLICATE))
        val vm = NicknameViewModel(getMyPage, check, update)
        advanceUntilIdle()
        vm.changeNickname("새_닉네임")
        vm.validate()
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()
        assertEquals(NicknameRejection.DUPLICATE, vm.uiState.value.rejection)
        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun `서버가 거절한 닉네임은 저장할 수 없다`() = runTest(dispatcher) {
        coEvery { check(any()) } returns Result.success(NicknameAvailability("금칙어", false, NicknameRejection.PROHIBITED_WORD))
        val vm = NicknameViewModel(getMyPage, check, update)
        advanceUntilIdle()
        vm.changeNickname("금칙어")
        vm.validate()
        advanceUntilIdle()
        assertEquals(NicknameRejection.PROHIBITED_WORD, vm.uiState.value.rejection)
        vm.save()
        coVerify(exactly = 0) { update(any()) }
    }
}
