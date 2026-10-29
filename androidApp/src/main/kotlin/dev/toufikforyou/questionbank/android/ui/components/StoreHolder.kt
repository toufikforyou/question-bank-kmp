package dev.toufikforyou.questionbank.android.ui.components

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.toufikforyou.questionbank.presentation.mvi.BaseStore

/** Ties a shared store to the Android ViewModel lifecycle so it survives configuration changes. */
class StoreHolder<T : BaseStore<*, *, *>>(val store: T) : ViewModel() {
    override fun onCleared() = store.close()
}

@Composable
inline fun <reified T : BaseStore<*, *, *>> rememberStore(noinline factory: () -> T): T {
    val holder: StoreHolder<T> = viewModel(key = T::class.java.name) { StoreHolder(factory()) }
    return holder.store
}
