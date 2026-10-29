import Foundation
import Shared

final class QuizViewModel: StoreViewModel<QuizState, QuizIntent, QuizEffect> {
    @Published var message: String?

    private let haptics: HapticsPerforming

    init(store: QuizStore, haptics: HapticsPerforming) {
        self.haptics = haptics
        super.init(store: store)
    }

    func startIfNeeded(_ request: QuizRequest) {
        guard state.status == .idle else { return }
        send(QuizIntent.Start(request: request))
    }

    func select(_ optionId: String) { send(QuizIntent.SelectOption(optionId: optionId)) }
    func next() { send(QuizIntent.Next.shared) }
    func toggleBookmark() { send(QuizIntent.ToggleBookmark.shared) }
    func restart() { send(QuizIntent.Restart.shared) }

    override func handle(effect: QuizEffect) {
        if let haptic = effect as? QuizEffect.Haptic {
            haptics.perform(haptic.kind)
        } else if let show = effect as? QuizEffect.ShowMessage {
            message = show.message
        }
    }
}
