package dev.toufikforyou.questionbank.presentation.mvi

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Minimal MVI store shared by Android and iOS.
 *
 * - [state] is a hot [StateFlow]; Compose collects it directly.
 * - [watchState]/[watchEffects] expose the same streams as plain callbacks so Swift
 *   `ObservableObject`s can observe them without any Flow interop library.
 * - One-off [effects] (haptics, messages) are delivered exactly once.
 */
abstract class BaseStore<S : Any, I : Any, E : Any>(
    initialState: S,
    dispatcher: CoroutineDispatcher,
) {
    protected val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<S> = mutableState.asStateFlow()

    private val effectChannel = Channel<E>(Channel.BUFFERED)
    val effects: Flow<E> = effectChannel.receiveAsFlow()

    val currentState: S get() = mutableState.value

    abstract fun dispatch(intent: I)

    protected fun setState(reducer: S.() -> S) = mutableState.update(reducer)

    protected fun emit(effect: E) {
        effectChannel.trySend(effect)
    }

    fun watchState(onChange: (S) -> Unit): StoreSubscription =
        StoreSubscription(scope.launch { state.collect { onChange(it) } })

    fun watchEffects(onEffect: (E) -> Unit): StoreSubscription =
        StoreSubscription(scope.launch { effects.collect { onEffect(it) } })

    open fun close() {
        scope.cancel()
        effectChannel.close()
    }
}

/** Handle returned to native observers; call [cancel] when the observer goes away. */
class StoreSubscription internal constructor(private val job: Job) {
    fun cancel() = job.cancel()
}
