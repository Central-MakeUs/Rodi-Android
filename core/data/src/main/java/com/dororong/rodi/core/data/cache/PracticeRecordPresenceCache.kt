package com.dororong.rodi.core.data.cache

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class PracticeRecordPresenceCache @Inject constructor() {
    private val mutex = Mutex()
    private val cacheLock = Any()

    @Volatile
    private var cachedValue: Boolean? = null

    private var generation = 0L

    fun get(): Boolean? = synchronized(cacheLock) { cachedValue }

    fun set(value: Boolean) {
        synchronized(cacheLock) {
            cachedValue = value
        }
    }

    fun clear() {
        synchronized(cacheLock) {
            generation++
            cachedValue = null
        }
    }

    /**
     * Returns the cached value or loads and caches one value.
     *
     * The loader runs while this cache is locked and must not call [withRefresh]
     * or another loading method on this cache.
     */
    suspend fun getOrLoad(loader: suspend () -> Boolean): Boolean =
        getOrLoadOrNull { loader() } ?: false

    /**
     * Like [getOrLoad], but a null result means that the loader could not prove
     * either state and must not be cached.
     */
    suspend fun getOrLoadOrNull(loader: suspend () -> Boolean?): Boolean? {
        get()?.let { return it }
        return mutex.withLock {
            synchronized(cacheLock) { cachedValue }?.let { return@withLock it }
            val loadGeneration = synchronized(cacheLock) { generation }
            val value = loader()
            synchronized(cacheLock) {
                // generation이 바뀌었다면(clear/withRefresh) 로딩 도중 캐시가 리셋됐다는
                // 뜻이고, cachedValue가 이미 채워져 있다면 동시 set()이 더 최신 값을 반영했다는
                // 뜻이다. 두 경우 모두 지연된 로더 결과로 덮어쓰지 않는다.
                if (generation == loadGeneration && cachedValue == null && value != null) {
                    cachedValue = value
                }
            }
            value
        }
    }

    /** The block must not call another method that acquires this cache's mutex. */
    suspend fun <T> withRefresh(block: suspend () -> T): T = mutex.withLock {
        val refreshGeneration = synchronized(cacheLock) {
            generation++
            cachedValue = null
            generation
        }
        try {
            block()
        } finally {
            synchronized(cacheLock) {
                if (generation != refreshGeneration) cachedValue = null
            }
        }
    }
}
