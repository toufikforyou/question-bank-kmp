package dev.toufikforyou.questionbank.data.cache

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe, suspend-aware lazy value used as an in-memory cache.
 * The first caller computes the value; concurrent callers wait on the same mutex
 * and every later read is lock-free.
 */
class SuspendLazy<T : Any>(private val initializer: suspend () -> T) {
    private val mutex = Mutex()
    private val holder = MutableStateFlow<T?>(null)

    val valueOrNull: T? get() = holder.value

    suspend fun get(): T = holder.value ?: mutex.withLock {
        holder.value ?: initializer().also { holder.value = it }
    }

    fun invalidate() {
        holder.value = null
    }
}
