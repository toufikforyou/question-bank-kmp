package dev.toufikforyou.questionbank.presentation.home

import dev.toufikforyou.questionbank.domain.model.Category
import dev.toufikforyou.questionbank.domain.model.UserStats
import dev.toufikforyou.questionbank.domain.usecase.GetCategoriesUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetStatsUseCase
import dev.toufikforyou.questionbank.domain.usecase.InitializeAppUseCase
import dev.toufikforyou.questionbank.presentation.mvi.BaseStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HomeState(
    val isLoading: Boolean = true,
    val profileName: String = "",
    val categories: List<Category> = emptyList(),
    val stats: UserStats = UserStats.EMPTY,
    val errorMessage: String? = null,
) {
    val hasContent: Boolean get() = categories.isNotEmpty()
    val totalQuestions: Int get() = categories.sumOf { it.questionCount }
}

sealed class HomeIntent {
    data object Retry : HomeIntent()
}

sealed class HomeEffect {
    data class ShowMessage(val message: String) : HomeEffect()
}

class HomeStore(
    private val initializeApp: InitializeAppUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val getStats: GetStatsUseCase,
    dispatcher: CoroutineDispatcher,
) : BaseStore<HomeState, HomeIntent, HomeEffect>(HomeState(), dispatcher) {

    private var loadJob: Job? = null

    init {
        load()
    }

    override fun dispatch(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Retry -> load()
        }
    }

    private fun load() {
        loadJob?.cancel()
        setState { copy(isLoading = true, errorMessage = null) }
        loadJob = scope.launch {
            val profile = initializeApp().getOrElse { error ->
                setState { copy(isLoading = false, errorMessage = error.message ?: GENERIC_ERROR) }
                return@launch
            }
            setState { copy(profileName = profile.displayName) }

            combine(getCategories(), getStats()) { categories, stats -> categories to stats }
                .catch { error ->
                    setState { copy(isLoading = false, errorMessage = error.message ?: GENERIC_ERROR) }
                    emit(HomeEffect.ShowMessage(error.message ?: GENERIC_ERROR))
                }
                .collect { (categories, stats) ->
                    setState { copy(isLoading = false, categories = categories, stats = stats, errorMessage = null) }
                }
        }
    }

    private companion object {
        const val GENERIC_ERROR = "Something went wrong while loading your question bank."
    }
}
