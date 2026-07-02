package com.dororong.rodi.core.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RunSuspendCatchingTest {

    @Test
    fun `returns success when block returns value`() = runTest {
        val result = runSuspendCatching { "done" }

        assertEquals(Result.success("done"), result)
    }

    @Test
    fun `wraps regular Throwable as failure`() = runTest {
        val exception = RuntimeException("boom")

        val result = runSuspendCatching<String> { throw exception }

        assertSame(exception, result.exceptionOrNull())
    }

    @Test
    fun `rethrows CancellationException instead of wrapping it`() = runTest {
        assertThrows<CancellationException> {
            runSuspendCatching { throw CancellationException("cancelled") }
        }
    }
}
