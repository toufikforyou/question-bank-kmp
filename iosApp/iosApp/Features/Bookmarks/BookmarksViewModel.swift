import Shared

final class BookmarksViewModel: StoreViewModel<BookmarksState, BookmarksIntent, BookmarksEffect> {
    private let haptics: HapticsPerforming

    init(store: BookmarksStore, haptics: HapticsPerforming) {
        self.haptics = haptics
        super.init(store: store)
    }

    func remove(_ questionId: String) {
        send(BookmarksIntent.Remove(questionId: questionId))
    }

    override func handle(effect: BookmarksEffect) {
        if let haptic = effect as? BookmarksEffect.Haptic {
            haptics.perform(haptic.kind)
        }
    }
}
