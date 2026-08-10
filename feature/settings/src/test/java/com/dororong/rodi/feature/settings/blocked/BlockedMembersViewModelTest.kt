package com.dororong.rodi.feature.settings.blocked

import com.dororong.rodi.core.domain.usecase.member.UnblockMemberUseCase
import com.dororong.rodi.core.domain.usecase.member.GetBlockedMembersUseCase
import com.dororong.rodi.core.domain.model.place.CursorPage
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
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
}
