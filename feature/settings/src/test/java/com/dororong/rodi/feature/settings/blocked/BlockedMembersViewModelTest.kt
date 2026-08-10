package com.dororong.rodi.feature.settings.blocked

import com.dororong.rodi.core.domain.usecase.member.UnblockMemberUseCase
import com.dororong.rodi.core.domain.usecase.member.GetBlockedMembersUseCase
import com.dororong.rodi.core.domain.model.place.CursorPage
import io.mockk.coEvery
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
    fun `successful unblock removes member`() = runTest(dispatcher) {
        coEvery { unblock(1) } returns Result.success(Unit)
        coEvery { getBlocked(any(), any()) } returns Result.success(CursorPage(emptyList(), false, null, 0))
        val viewModel = BlockedMembersViewModel(unblock, getBlocked)
        viewModel.replaceMembers(listOf(member))
        viewModel.unblock(member)
        advanceUntilIdle()
        assertEquals(emptyList<BlockedMember>(), viewModel.uiState.value.members)
    }

    @Test
    fun `failed unblock keeps member and exposes error`() = runTest(dispatcher) {
        coEvery { unblock(1) } returns Result.failure(IllegalStateException("실패"))
        coEvery { getBlocked(any(), any()) } returns Result.success(CursorPage(emptyList(), false, null, 0))
        val viewModel = BlockedMembersViewModel(unblock, getBlocked)
        viewModel.replaceMembers(listOf(member))
        viewModel.unblock(member)
        advanceUntilIdle()
        assertEquals(listOf(member), viewModel.uiState.value.members)
        assertEquals("실패", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `cancellation during unblock does not become a user error`() = runTest(dispatcher) {
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
    fun `late page response does not restore an unblocked member`() = runTest(dispatcher) {
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
