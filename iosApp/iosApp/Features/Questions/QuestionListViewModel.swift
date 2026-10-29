import Foundation
import Shared

final class QuestionListViewModel: StoreViewModel<QuestionListState, QuestionListIntent, QuestionListEffect> {
    @Published var message: String?

    private let haptics: HapticsPerforming

    init(store: QuestionListStore, haptics: HapticsPerforming) {
        self.haptics = haptics
        super.init(store: store)
    }

    func search(_ query: String) { send(QuestionListIntent.UpdateQuery(query: query)) }
    func filter(_ filter: QuestionFilter) { send(QuestionListIntent.SetFilter(filter: filter)) }
    func select(questionId: String, optionId: String) { send(QuestionListIntent.SelectOption(questionId: questionId, optionId: optionId)) }
    func toggleReveal(_ questionId: String) { send(QuestionListIntent.ToggleReveal(questionId: questionId)) }
    func toggleBookmark(_ questionId: String) { send(QuestionListIntent.ToggleBookmark(questionId: questionId)) }
    func retry() { send(QuestionListIntent.Retry.shared) }

    override func handle(effect: QuestionListEffect) {
        if let haptic = effect as? QuestionListEffect.Haptic {
            haptics.perform(haptic.kind)
        } else if let show = effect as? QuestionListEffect.ShowMessage {
            message = show.message
        }
    }
}
