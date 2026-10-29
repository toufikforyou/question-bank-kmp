import SwiftUI
import Shared

/// Value-type copy of a Kotlin `PaperKey`, so routes stay plain Swift `Hashable`s.
struct PaperRef: Hashable {
    let subjectId: String
    let boardId: String
    let year: Int32

    init(_ key: PaperKey) {
        subjectId = key.subjectId
        boardId = key.boardId
        year = key.year
    }

    var key: PaperKey { PaperKey(subjectId: subjectId, boardId: boardId, year: year) }
}

enum Route: Hashable {
    case subject(String)
    case paper(PaperRef)
    case chapter(String)
    case search(ExamType)
    case bookmarks
    case stats
    case quiz(QuizRequest)
}

struct RootView: View {
    let dependencies: AppDependencies
    @State private var path: [Route] = []

    var body: some View {
        NavigationStack(path: $path) {
            HomeView(dependencies: dependencies) { path.append($0) }
                .navigationDestination(for: Route.self) { route in
                    destination(for: route)
                }
        }
    }

    @ViewBuilder
    private func destination(for route: Route) -> some View {
        let container = dependencies.container
        let navigate: (Route) -> Void = { path.append($0) }
        switch route {
        case .subject(let id):
            SubjectView(store: container.subjectStore(subjectId: id), navigate: navigate)
        case .paper(let ref):
            QuestionListView(store: container.paperStore(key: ref.key), haptics: dependencies.haptics, navigate: navigate)
        case .chapter(let id):
            QuestionListView(store: container.chapterStore(chapterId: id), haptics: dependencies.haptics, navigate: navigate)
        case .search(let exam):
            QuestionListView(store: container.searchStore(exam: exam), haptics: dependencies.haptics, navigate: navigate)
        case .bookmarks:
            QuestionListView(store: container.bookmarksStore(), haptics: dependencies.haptics, navigate: navigate)
        case .stats:
            StatsView(dependencies: dependencies)
        case .quiz(let request):
            QuizView(dependencies: dependencies, request: request) { if !path.isEmpty { path.removeLast() } }
        }
    }
}
