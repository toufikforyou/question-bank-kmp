import Shared

final class SubjectViewModel: StoreViewModel<SubjectState, SubjectIntent, SubjectEffect> {
    init(store: SubjectStore) {
        super.init(store: store)
    }

    func select(tab: SubjectTab) { send(SubjectIntent.SelectTab(tab: tab)) }
    func filter(boardId: String?) { send(SubjectIntent.FilterBoard(boardId: boardId)) }
    func retry() { send(SubjectIntent.Retry.shared) }
}
