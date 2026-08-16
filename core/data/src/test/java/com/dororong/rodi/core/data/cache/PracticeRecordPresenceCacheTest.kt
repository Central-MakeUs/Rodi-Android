package com.dororong.rodi.core.data.cache

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PracticeRecordPresenceCacheTest {
    @Test
    fun `clear during a load prevents the stale result from being cached`() = runTest {
        val cache = PracticeRecordPresenceCache()
        val loaderStarted = CompletableDeferred<Unit>()
        val releaseLoader = CompletableDeferred<Unit>()

        val loading = async {
            cache.getOrLoadOrNull {
                loaderStarted.complete(Unit)
                releaseLoader.await()
                true
            }
        }
        loaderStarted.await()

        cache.clear()
        releaseLoader.complete(Unit)

        assertTrue(loading.await() == true)
        assertNull(cache.get())
    }
}
