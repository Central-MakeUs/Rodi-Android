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

    suspend fun getOrLoad(loader: suspend () -> Boolean): Boolean {
        cachedValue?.let { return it }
        return mutex.withLock {
            cachedValue?.let { return@withLock it }
            loader().also(::set)
        }
    }

    suspend fun <T> withRefresh(block: suspend () -> T): T = mutex.withLock { block() }
}
