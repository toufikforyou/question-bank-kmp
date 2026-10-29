import Shared

final class StatsViewModel: StoreViewModel<StatsState, StatsIntent, StatsEffect> {
    init(store: StatsStore) {
        super.init(store: store)
    }
}
