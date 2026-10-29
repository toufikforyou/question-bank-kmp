import Shared

final class HomeViewModel: StoreViewModel<HomeState, HomeIntent, HomeEffect> {
    init(store: HomeStore) {
        super.init(store: store)
    }

    func retry() {
        send(HomeIntent.Retry.shared)
    }
}
