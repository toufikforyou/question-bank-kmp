import SwiftUI
import Shared

struct BookmarksView: View {
    @StateObject private var viewModel: BookmarksViewModel
    private let onPractice: () -> Void

    init(dependencies: AppDependencies, onPractice: @escaping () -> Void) {
        _viewModel = StateObject(
            wrappedValue: BookmarksViewModel(store: dependencies.container.bookmarksStore(), haptics: dependencies.haptics)
        )
        self.onPractice = onPractice
    }

    var body: some View {
        Group {
            let state = viewModel.state
            if state.isLoading {
                ProgressView()
            } else if state.isEmpty {
                EmptyStateView(
                    systemImage: "bookmark",
                    title: "No bookmarks yet",
                    message: "Tap the bookmark icon (or double-tap a card) during a quiz to save it for later."
                )
            } else {
                List {
                    Section {
                        Button(action: onPractice) {
                            Label("Practise \(state.questions.count) bookmarked", systemImage: "play.fill")
                        }
                        .accessibilityIdentifier("bookmarks.practice")
                    }
                    Section {
                        ForEach(state.questions, id: \.id) { question in
                            VStack(alignment: .leading, spacing: 4) {
                                Text(question.prompt).font(.body)
                                Text("Answer: \(question.correctOption.text)")
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                            }
                            .padding(.vertical, 4)
                            .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                                Button(role: .destructive) {
                                    viewModel.remove(question.id)
                                } label: {
                                    Label("Remove", systemImage: "bookmark.slash")
                                }
                            }
                            .accessibilityIdentifier("bookmarks.row.\(question.id)")
                        }
                    } footer: {
                        Text("Swipe left to remove a bookmark.")
                    }
                }
                .animation(.default, value: state.questions.count)
            }
        }
        .navigationTitle("Bookmarks")
    }
}
