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
                if (generation == loadGeneration) {
                    if (value != null) cachedValue = value
                } else {
                    cachedValue = null
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
