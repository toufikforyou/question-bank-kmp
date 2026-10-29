import Shared

final class HomeViewModel: StoreViewModel<HomeState, HomeIntent, HomeEffect> {
    init(store: HomeStore) {
        super.init(store: store)
    }

    func retry() { send(HomeIntent.Retry.shared) }
    func selectExam(_ exam: ExamType) { send(HomeIntent.SelectExam(exam: exam)) }
    func selectSubject(_ id: String) { send(HomeIntent.SelectFinderSubject(subjectId: id)) }
    func selectBoard(_ id: String) { send(HomeIntent.SelectFinderBoard(boardId: id)) }
    func selectYear(_ year: Int32) { send(HomeIntent.SelectFinderYear(year: year)) }
}
