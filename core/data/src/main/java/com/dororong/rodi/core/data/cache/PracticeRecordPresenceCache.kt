package com.dororong.rodi.core.data.cache

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class PracticeRecordPresenceCache @Inject constructor() {
    private val mutex = Mutex()

    @Volatile
    private var cachedValue: Boolean? = null

    fun get(): Boolean? = cachedValue

    fun set(value: Boolean) {
        cachedValue = value
    }

    fun clear() {
        cachedValue = null
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
        cachedValue?.let { return it }
        return mutex.withLock {
            cachedValue?.let { return@withLock it }
            val value = loader()
            if (value != null) set(value)
            value
        }
    }

    /** The block must not call another method that acquires this cache's mutex. */
    suspend fun <T> withRefresh(block: suspend () -> T): T = mutex.withLock { block() }
}
