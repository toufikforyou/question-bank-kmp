import SwiftUI

struct QuizConfig: Hashable {
    let categoryId: String?
    let title: String
    let bookmarkedOnly: Bool

    static let quick = QuizConfig(categoryId: nil, title: "Quick quiz", bookmarkedOnly: false)
    static let bookmarks = QuizConfig(categoryId: nil, title: "Bookmarked", bookmarkedOnly: true)
}

enum Route: Hashable {
    case quiz(QuizConfig)
    case bookmarks
    case stats
}

struct RootView: View {
    let dependencies: AppDependencies
    @State private var path: [Route] = []

    var body: some View {
        NavigationStack(path: $path) {
            HomeView(dependencies: dependencies) { path.append($0) }
                .navigationDestination(for: Route.self) { route in
                    switch route {
                    case .quiz(let config):
                        QuizView(dependencies: dependencies, config: config) { path.removeAll() }
                    case .bookmarks:
                        BookmarksView(dependencies: dependencies) { path.append(.quiz(.bookmarks)) }
                    case .stats:
                        StatsView(dependencies: dependencies)
                    }
                }
        }
    }
}
