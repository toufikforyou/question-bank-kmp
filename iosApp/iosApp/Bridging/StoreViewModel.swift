import Combine
import Shared

/// Bridges a Kotlin `BaseStore` to SwiftUI.
///
/// The Kotlin store keeps its state in a `StateFlow`; `watchState` delivers every emission on the
/// main thread (the store runs on `Dispatchers.Main`), and we republish it through `@Published`
/// so any `@StateObject` / `@ObservedObject` re-renders. One-off effects are routed to `handle(effect:)`.
class StoreViewModel<S: AnyObject, I: AnyObject, E: AnyObject>: ObservableObject {
    @Published private(set) var state: S

    let store: BaseStore<S, I, E>
    private var stateSubscription: StoreSubscription?
    private var effectSubscription: StoreSubscription?

    init(store: BaseStore<S, I, E>) {
        self.store = store
        self.state = store.currentState
        stateSubscription = store.watchState { [weak self] newState in
            self?.state = newState
        }
        effectSubscription = store.watchEffects { [weak self] effect in
            self?.handle(effect: effect)
        }
    }

    func send(_ intent: I) {
        store.dispatch(intent: intent)
    }

    /// Override to react to one-off effects such as haptics or messages.
    func handle(effect: E) {}

    deinit {
        stateSubscription?.cancel()
        effectSubscription?.cancel()
        store.close()
    }
}
