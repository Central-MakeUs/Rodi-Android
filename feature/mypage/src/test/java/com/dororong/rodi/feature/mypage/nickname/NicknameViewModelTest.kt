package com.dororong.rodi.feature.mypage.nickname

import app.cash.turbine.test
import com.dororong.rodi.core.domain.model.member.MyPage
import com.dororong.rodi.core.domain.model.member.NicknameAvailability
import com.dororong.rodi.core.domain.model.member.NicknameRejectedException
import com.dororong.rodi.core.domain.model.member.NicknameRejection
import com.dororong.rodi.core.domain.model.member.NicknameUpdate
import java.time.LocalDateTime
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
        val response = CompletableDeferred<Result<NicknameUpdate>>()
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
        response.complete(Result.success(NicknameUpdate("새닉네임")))
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
        coEvery { update(any()) } returns Result.success(NicknameUpdate("새_닉네임"))
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
    @Test
    fun `현재와 같은 닉네임은 검증 요청과 저장을 하지 않는다`() = runTest(dispatcher) {
        coEvery { check("기존_닉네임") } returns Result.success(NicknameAvailability("기존_닉네임", true, null))
        val vm = NicknameViewModel(getMyPage, check, update)
        advanceUntilIdle()

        vm.validate()
        advanceUntilIdle()

        coVerify(exactly = 0) { check(any()) }
        assertFalse(vm.uiState.value.canSave)
        vm.save()
        coVerify(exactly = 0) { update(any()) }
    }

    @Test
    fun `검증 API의 도메인 거절은 입력 안내로 표시하고 일반 오류를 보내지 않는다`() = runTest(dispatcher) {
        coEvery { check("다른_닉네임") } returns Result.failure(NicknameRejectedException(NicknameRejection.DUPLICATE))
        val vm = NicknameViewModel(getMyPage, check, update)
        advanceUntilIdle()
        vm.changeNickname("다른_닉네임")

        vm.effect.test {
            vm.validate()
            advanceUntilIdle()

            assertEquals(NicknameRejection.DUPLICATE, vm.uiState.value.rejection)
            assertFalse(vm.uiState.value.canSave)
            expectNoEvents()
        }
    }

    @Test
    fun `프로필의 과거 제한 시각도 클라이언트에서 만료 계산 없이 제한으로 보존한다`() = runTest(dispatcher) {
        val restriction = LocalDateTime.of(2000, 1, 2, 3, 4)
        coEvery { getMyPage() } returns Result.success(page(restriction))

        val vm = NicknameViewModel(getMyPage, check, update)
        advanceUntilIdle()

        assertEquals(restriction, vm.uiState.value.nicknameChangeableAt)
        assertTrue(vm.uiState.value.isChangeRestricted)
        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun `프로필의 제한 시각이 null이면 새 후보를 검증하고 저장할 수 있다`() = runTest(dispatcher) {
        coEvery { getMyPage() } returns Result.success(page(null))
        coEvery { check("Different") } returns Result.success(NicknameAvailability("Different", true, null))
        val vm = NicknameViewModel(getMyPage, check, update)
        advanceUntilIdle()
        vm.changeNickname("Different")
        vm.validate()
        advanceUntilIdle()

        assertNull(vm.uiState.value.nicknameChangeableAt)
        assertFalse(vm.uiState.value.isChangeRestricted)
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun `제한 중 대소문자만 다른 후보의 허용은 기존 제한 시각을 해제하지 않는다`() = runTest(dispatcher) {
        val restriction = LocalDateTime.of(2026, 11, 20, 12, 30)
        coEvery { getMyPage() } returns Result.success(page(restriction))
        coEvery { check("rodi_user") } returns Result.success(NicknameAvailability("rodi_user", true, null))
        val vm = NicknameViewModel(getMyPage, check, update)
        advanceUntilIdle()
        vm.changeNickname("rodi_user")
        vm.validate()
        advanceUntilIdle()

        assertEquals(restriction, vm.uiState.value.nicknameChangeableAt)
        assertTrue(vm.uiState.value.isChangeRestricted)
        assertTrue(vm.uiState.value.canSave)
        coVerify(exactly = 1) { check("rodi_user") }
        vm.changeNickname("Different")
        assertTrue(vm.uiState.value.isChangeRestricted)
        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun `제한 중 허용된 대소문자 변경은 같은 입력을 다시 검증하지 않는다`() = runTest(dispatcher) {
        val restriction = LocalDateTime.of(2026, 11, 20, 12, 30)
        coEvery { getMyPage() } returns Result.success(page(restriction))
        coEvery { check("rodi_user") } returns Result.success(NicknameAvailability("rodi_user", true, null))
        val vm = NicknameViewModel(getMyPage, check, update)
        advanceUntilIdle()
        vm.changeNickname("rodi_user")
        vm.validate()
        advanceUntilIdle()

        vm.validate()
        advanceUntilIdle()

        coVerify(exactly = 1) { check("rodi_user") }
        assertTrue(vm.uiState.value.canSave)
        assertEquals(restriction, vm.uiState.value.nicknameChangeableAt)
    }

    @Test
    fun `제한 중 새 후보를 서버가 허용하면 제한 시각을 해제한다`() = runTest(dispatcher) {
        val restriction = LocalDateTime.of(2000, 1, 2, 3, 4)
        coEvery { getMyPage() } returns Result.success(page(restriction))
        coEvery { check("Different") } returns Result.success(NicknameAvailability("Different", true, null))
        val vm = NicknameViewModel(getMyPage, check, update)
        advanceUntilIdle()
        vm.changeNickname("Different")
        vm.validate()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isChangeRestricted)
        assertNull(vm.uiState.value.nicknameChangeableAt)
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun `사용 가능 여부의 제한 거절 후 같은 입력을 다시 검증해 해제할 수 있다`() = runTest(dispatcher) {
        val restriction = LocalDateTime.of(2026, 11, 20, 12, 30)
        coEvery { check("Different") } returnsMany listOf(
            Result.success(NicknameAvailability("Different", false, NicknameRejection.CHANGE_RESTRICTED, restriction)),
            Result.success(NicknameAvailability("Different", true, null)),
        )
        val vm = NicknameViewModel(getMyPage, check, update)
        advanceUntilIdle()
        vm.changeNickname("Different")
        vm.effect.test {
            vm.validate()
            advanceUntilIdle()
            assertEquals(NicknameRejection.CHANGE_RESTRICTED, vm.uiState.value.rejection)
            assertEquals(restriction, vm.uiState.value.nicknameChangeableAt)
            assertTrue(vm.uiState.value.isChangeRestricted)
            assertNull(vm.uiState.value.validatedNickname)
            assertFalse(vm.uiState.value.canSave)
            expectNoEvents()

            vm.validate()
            advanceUntilIdle()
            assertTrue(vm.uiState.value.canSave)
            assertFalse(vm.uiState.value.isChangeRestricted)
            assertNull(vm.uiState.value.nicknameChangeableAt)
            coVerify(exactly = 2) { check("Different") }
            expectNoEvents()
        }
    }

    @Test
    fun `검증 API의 제한 예외는 일반 오류 없이 시각을 보존하고 같은 후보의 재검증을 허용한다`() = runTest(dispatcher) {
        val restriction = LocalDateTime.of(2026, 11, 20, 12, 30)
        coEvery { check("Different") } returnsMany listOf(
            Result.failure(NicknameRejectedException(NicknameRejection.CHANGE_RESTRICTED, restriction)),
            Result.success(NicknameAvailability("Different", true, null)),
        )
        val vm = NicknameViewModel(getMyPage, check, update)
        advanceUntilIdle()
        vm.changeNickname("Different")
        vm.effect.test {
            vm.validate()
            advanceUntilIdle()
            assertTrue(vm.uiState.value.isChangeRestricted)
            assertEquals(restriction, vm.uiState.value.nicknameChangeableAt)
            assertNull(vm.uiState.value.validatedNickname)
            assertFalse(vm.uiState.value.canSave)
            expectNoEvents()

            vm.validate()
            advanceUntilIdle()
            assertTrue(vm.uiState.value.canSave)
            coVerify(exactly = 2) { check("Different") }
            expectNoEvents()
        }
    }

    @Test
    fun `저장 API의 제한 예외는 일반 오류 없이 저장을 막고 재검증으로 복구한다`() = runTest(dispatcher) {
        val restriction = LocalDateTime.of(2026, 11, 20, 12, 30)
        coEvery { check("Different") } returns Result.success(NicknameAvailability("Different", true, null))
        coEvery { update("Different") } returnsMany listOf(
            Result.failure(NicknameRejectedException(NicknameRejection.CHANGE_RESTRICTED, restriction)),
            Result.success(NicknameUpdate("Different", restriction)),
        )
        val vm = NicknameViewModel(getMyPage, check, update)
        advanceUntilIdle()
        vm.changeNickname("Different")
        vm.validate()
        advanceUntilIdle()
        vm.effect.test {
            vm.save()
            advanceUntilIdle()
            assertTrue(vm.uiState.value.isChangeRestricted)
            assertEquals(restriction, vm.uiState.value.nicknameChangeableAt)
            assertNull(vm.uiState.value.validatedNickname)
            assertFalse(vm.uiState.value.canSave)
            expectNoEvents()
            vm.save()
            coVerify(exactly = 1) { update("Different") }

            vm.validate()
            advanceUntilIdle()
            assertTrue(vm.uiState.value.canSave)
            vm.save()
            advanceUntilIdle()
            assertTrue(vm.uiState.value.saveSucceeded)
            assertEquals("Different", vm.uiState.value.initialNickname)
            assertEquals(restriction, vm.uiState.value.nicknameChangeableAt)
            coVerify(exactly = 2) { update("Different") }
            expectNoEvents()
        }
    }

    @Test
    fun `시각 없는 제한 예외도 입력을 바꾼 뒤 제한 안내를 유지한다`() = runTest(dispatcher) {
        coEvery { check("Different") } returns Result.failure(NicknameRejectedException(NicknameRejection.CHANGE_RESTRICTED))
        val vm = NicknameViewModel(getMyPage, check, update)
        advanceUntilIdle()
        vm.changeNickname("Different")
        vm.validate()
        advanceUntilIdle()
        vm.changeNickname("Another")

        assertTrue(vm.uiState.value.isChangeRestricted)
        assertNull(vm.uiState.value.nicknameChangeableAt)
        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun `이전 후보의 늦은 제한 응답은 현재 후보와 제한 상태를 덮지 않는다`() = runTest(dispatcher) {
        val response = CompletableDeferred<Result<NicknameAvailability>>()
        coEvery { check("First") } coAnswers { withContext(NonCancellable) { response.await() } }
        coEvery { check("Different") } returns Result.success(NicknameAvailability("Different", true, null))
        val vm = NicknameViewModel(getMyPage, check, update)
        advanceUntilIdle()
        vm.changeNickname("First")
        vm.validate()
        runCurrent()
        vm.changeNickname("Different")
        vm.validate()
        advanceUntilIdle()
        response.complete(Result.failure(NicknameRejectedException(NicknameRejection.CHANGE_RESTRICTED, LocalDateTime.of(2026, 11, 20, 12, 30))))
        advanceUntilIdle()

        assertEquals("Different", vm.uiState.value.nickname)
        assertTrue(vm.uiState.value.canSave)
        assertFalse(vm.uiState.value.isChangeRestricted)
        assertNull(vm.uiState.value.nicknameChangeableAt)
    }

    @Test
    fun `늦은 프로필 응답은 새로 조회한 닉네임과 제한 상태를 되돌리지 않는다`() = runTest(dispatcher) {
        val first = CompletableDeferred<Result<MyPage>>()
        coEvery { getMyPage() } coAnswers { first.await() }
        val vm = NicknameViewModel(getMyPage, check, update)
        runCurrent()
        coEvery { getMyPage() } returns Result.success(page(null).copy(nickname = "Fresh"))
        vm.load()
        runCurrent()
        first.complete(Result.success(page(LocalDateTime.of(2026, 11, 20, 12, 30))))
        advanceUntilIdle()

        assertEquals("Fresh", vm.uiState.value.nickname)
        assertFalse(vm.uiState.value.isChangeRestricted)
        assertNull(vm.uiState.value.nicknameChangeableAt)
    }

    @Test
    fun `서버 저장 이름과 제한 시각을 보존하고 재진입에서 같은 값을 조회한다`() = runTest(dispatcher) {
        val restriction = LocalDateTime.of(2026, 11, 20, 12, 30)
        coEvery { getMyPage() } returns Result.success(page(null))
        coEvery { check("Different") } returns Result.success(NicknameAvailability("Different", true, null))
        coEvery { update("Different") } returns Result.success(NicknameUpdate("Canonical", restriction))
        val vm = NicknameViewModel(getMyPage, check, update)
        advanceUntilIdle()
        vm.changeNickname("Different")
        vm.validate()
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.saveSucceeded)
        assertEquals("Canonical", vm.uiState.value.nickname)
        assertEquals("Canonical", vm.uiState.value.initialNickname)
        assertEquals(restriction, vm.uiState.value.nicknameChangeableAt)
        assertTrue(vm.uiState.value.isChangeRestricted)

        coEvery { getMyPage() } returns Result.success(page(restriction).copy(nickname = "Canonical"))
        val reentered = NicknameViewModel(getMyPage, check, update)
        advanceUntilIdle()
        assertEquals(vm.uiState.value.nickname, reentered.uiState.value.nickname)
        assertEquals(restriction, reentered.uiState.value.nicknameChangeableAt)
        assertTrue(reentered.uiState.value.isChangeRestricted)
    }

    @Test
    fun `제한 거절 뒤 같은 새 후보의 중복 응답은 계정 제한을 해제하고 중복 사유를 보존한다`() = runTest(dispatcher) {
        val restriction = LocalDateTime.of(2000, 1, 2, 3, 4)
        coEvery { getMyPage() } returns Result.success(page(restriction))
        coEvery { check("Different") } returnsMany listOf(
            Result.success(NicknameAvailability("Different", false, NicknameRejection.CHANGE_RESTRICTED, restriction)),
            Result.success(NicknameAvailability("Different", false, NicknameRejection.DUPLICATE)),
        )
        val vm = NicknameViewModel(getMyPage, check, update)
        advanceUntilIdle()
        vm.changeNickname("Different")
        vm.validate()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isChangeRestricted)

        vm.validate()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isChangeRestricted)
        assertNull(vm.uiState.value.nicknameChangeableAt)
        assertEquals(NicknameRejection.DUPLICATE, vm.uiState.value.rejection)
        assertFalse(vm.uiState.value.canSave)
        coVerify(exactly = 2) { check("Different") }
    }

    @Test
    fun `새 후보의 비제한 도메인 거절도 기존 계정 제한을 해제한다`() = runTest(dispatcher) {
        val restriction = LocalDateTime.of(2000, 1, 2, 3, 4)
        coEvery { getMyPage() } returns Result.success(page(restriction))
        coEvery { check("Different") } returns Result.failure(NicknameRejectedException(NicknameRejection.DUPLICATE))
        val vm = NicknameViewModel(getMyPage, check, update)
        advanceUntilIdle()
        vm.changeNickname("Different")

        vm.validate()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isChangeRestricted)
        assertNull(vm.uiState.value.nicknameChangeableAt)
        assertEquals(NicknameRejection.DUPLICATE, vm.uiState.value.rejection)
        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun `중복으로 거절된 입력은 수정 없이 다시 검증하고 사용 가능해질 수 있다`() = runTest(dispatcher) {
        coEvery { check("Different") } returnsMany listOf(
            Result.success(NicknameAvailability("Different", false, NicknameRejection.DUPLICATE)),
            Result.success(NicknameAvailability("Different", true, null)),
        )
        val vm = NicknameViewModel(getMyPage, check, update)
        advanceUntilIdle()
        vm.changeNickname("Different")
        vm.validate()
        advanceUntilIdle()
        assertEquals(NicknameRejection.DUPLICATE, vm.uiState.value.rejection)

        vm.validate()
        advanceUntilIdle()

        coVerify(exactly = 2) { check("Different") }
        assertNull(vm.uiState.value.rejection)
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun `저장 중 중복으로 거절된 입력도 수정 없이 다시 검증할 수 있다`() = runTest(dispatcher) {
        coEvery { check("Different") } returns Result.success(NicknameAvailability("Different", true, null))
        coEvery { update("Different") } returns Result.failure(NicknameRejectedException(NicknameRejection.DUPLICATE))
        val vm = NicknameViewModel(getMyPage, check, update)
        advanceUntilIdle()
        vm.changeNickname("Different")
        vm.validate()
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()
        assertEquals(NicknameRejection.DUPLICATE, vm.uiState.value.rejection)

        vm.validate()
        advanceUntilIdle()

        coVerify(exactly = 2) { check("Different") }
        assertNull(vm.uiState.value.rejection)
        assertTrue(vm.uiState.value.canSave)
    }

    private fun page(restriction: LocalDateTime?) = MyPage(
        nickname = "Rodi_user",
        level = OnboardingLevel.SEED,
        recommendationTags = emptyList(),
        drivingGoal = null,
        savedPlaceCount = 0,
        nicknameChangeableAt = restriction,
    )

}
