package com.dororong.rodi.core.domain.usecase.member

import com.dororong.rodi.core.domain.repository.MemberRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MemberBlockUseCasesTest {
    private val repository = mockk<MemberRepository>()

    @Test
    fun `회원 차단을 저장소에 한 번 위임한다`() = runTest {
        coEvery { repository.blockMember(7) } returns Unit

        val result = BlockMemberUseCase(repository)(7)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.blockMember(7) }
    }

    @Test
    fun `회원 차단 해제를 저장소에 한 번 위임한다`() = runTest {
        coEvery { repository.unblockMember(7) } returns Unit

        val result = UnblockMemberUseCase(repository)(7)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.unblockMember(7) }
    }
}
