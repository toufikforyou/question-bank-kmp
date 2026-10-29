package dev.toufikforyou.questionbank.presentation.stats

import dev.toufikforyou.questionbank.domain.model.UserStats
import dev.toufikforyou.questionbank.domain.usecase.GetStatsUseCase
import dev.toufikforyou.questionbank.presentation.mvi.BaseStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

data class StatsState(
    val isLoading: Boolean = true,
    val stats: UserStats = UserStats.EMPTY,
)

sealed class StatsIntent

sealed class StatsEffect

class StatsStore(
    getStats: GetStatsUseCase,
    dispatcher: CoroutineDispatcher,
) : BaseStore<StatsState, StatsIntent, StatsEffect>(StatsState(), dispatcher) {

    init {
        scope.launch {
            getStats().collect { stats -> setState { copy(isLoading = false, stats = stats) } }
        }
    }

    override fun dispatch(intent: StatsIntent) = Unit
}
