package com.dororong.rodi.core.data.cache

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PracticeRecordPresenceCacheTest {
    @Test
    fun `로드 중 초기화하면 이전 응답을 캐시에 저장하지 않는다`() = runTest {
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

    @Test
    fun `로드 중 설정한 값은 늦게 끝난 로드 결과로 덮어쓰지 않는다`() = runTest {
        val cache = PracticeRecordPresenceCache()
        val loaderStarted = CompletableDeferred<Unit>()
        val releaseLoader = CompletableDeferred<Unit>()

        val loading = async {
            cache.getOrLoadOrNull {
                loaderStarted.complete(Unit)
                releaseLoader.await()
                false
            }
        }
        loaderStarted.await()

        cache.set(true)
        releaseLoader.complete(Unit)

        loading.await()
        assertTrue(cache.get() == true)
    }
}
