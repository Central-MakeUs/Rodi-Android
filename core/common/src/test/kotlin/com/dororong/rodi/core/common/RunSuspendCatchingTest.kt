package com.dororong.rodi.core.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RunSuspendCatchingTest {

    @Test
    fun `블록이 값을 반환하면 성공 결과를 반환한다`() = runTest {
        val result = runSuspendCatching { "done" }

        assertEquals(Result.success("done"), result)
    }

    @Test
    fun `일반 예외는 실패 결과로 감싼다`() = runTest {
        val exception = RuntimeException("boom")

        val result = runSuspendCatching<String> { throw exception }

        assertSame(exception, result.exceptionOrNull())
    }

    @Test
    fun `CancellationException은 감싸지 않고 다시 던진다`() = runTest {
        assertThrows<CancellationException> {
            runSuspendCatching { throw CancellationException("cancelled") }
        }
    }
}
