package com.dororong.rodi.feature.settings.blocked

import com.dororong.rodi.core.domain.usecase.member.UnblockMemberUseCase
import com.dororong.rodi.core.domain.usecase.member.GetBlockedMembersUseCase
import com.dororong.rodi.core.domain.model.place.CursorPage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BlockedMembersViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val unblock = mockk<UnblockMemberUseCase>()
    private val getBlocked = mockk<GetBlockedMembersUseCase>()
    private val member = BlockedMember(1, "차단된 사용자", java.time.Instant.EPOCH)

    @BeforeEach fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterEach fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `차단 해제에 성공하면 회원을 목록에서 지운다`() = runTest(dispatcher) {
        coEvery { unblock(1) } returns Result.success(Unit)
        coEvery { getBlocked(any(), any()) } returns Result.success(CursorPage(emptyList(), false, null, 0))
        val viewModel = BlockedMembersViewModel(unblock, getBlocked)
        viewModel.replaceMembers(listOf(member))
        viewModel.unblock(member)
        advanceUntilIdle()
        assertEquals(emptyList<BlockedMember>(), viewModel.uiState.value.members)
    }

    @Test
    fun `차단 해제가 실패하면 회원을 유지하고 오류를 보여준다`() = runTest(dispatcher) {
        coEvery { unblock(1) } returns Result.failure(IllegalStateException("실패"))
        coEvery { getBlocked(any(), any()) } returns Result.success(CursorPage(emptyList(), false, null, 0))
        val viewModel = BlockedMembersViewModel(unblock, getBlocked)
        viewModel.replaceMembers(listOf(member))
        viewModel.unblock(member)
        advanceUntilIdle()
        assertEquals(listOf(member), viewModel.uiState.value.members)
        assertEquals("차단을 해제하지 못했어요.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `차단 해제 중 취소는 사용자 오류로 보여주지 않는다`() = runTest(dispatcher) {
        coEvery { unblock(1) } coAnswers { throw CancellationException("취소") }
        coEvery { getBlocked(any(), any()) } returns Result.success(CursorPage(emptyList(), false, null, 0))
        val viewModel = BlockedMembersViewModel(unblock, getBlocked)
        viewModel.replaceMembers(listOf(member))

        viewModel.unblock(member)
        try {
            advanceUntilIdle()
        } catch (_: CancellationException) {
        }

        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `차단 해제를 여러 번 눌러도 진행 중인 요청 하나를 공유한다`() = runTest(dispatcher) {
        val completion = CompletableDeferred<Result<Unit>>()
        coEvery { unblock(1) } coAnswers { completion.await() }
        coEvery { getBlocked(any(), any()) } returns Result.success(CursorPage(emptyList(), false, null, 0))
        val viewModel = BlockedMembersViewModel(unblock, getBlocked)
        viewModel.replaceMembers(listOf(member))

        viewModel.unblock(member)
        viewModel.unblock(member)
        advanceUntilIdle()

        coVerify(exactly = 1) { unblock(1) }
        completion.complete(Result.success(Unit))
        advanceUntilIdle()
        assertEquals(emptyList<BlockedMember>(), viewModel.uiState.value.members)
    }

    @Test
    fun `늦게 도착한 페이지 응답은 이미 차단 해제한 사용자를 다시 추가하지 않는다`() = runTest(dispatcher) {
        val page = CompletableDeferred<Result<CursorPage<BlockedMember>>>()
        coEvery { getBlocked(null, 20) } returns Result.success(
            CursorPage(listOf(member), true, "next", 2),
        )
        coEvery { getBlocked("next", 20) } coAnswers { page.await() }
        coEvery { unblock(1) } returns Result.success(Unit)
        val viewModel = BlockedMembersViewModel(unblock, getBlocked)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()
        viewModel.unblock(member)
        advanceUntilIdle()
        page.complete(Result.success(CursorPage(listOf(member), false, null, 1)))
        advanceUntilIdle()

        assertEquals(emptyList<BlockedMember>(), viewModel.uiState.value.members)
    }
}
