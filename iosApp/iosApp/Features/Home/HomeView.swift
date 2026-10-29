import SwiftUI
import Shared

struct HomeView: View {
    @StateObject private var viewModel: HomeViewModel
    private let navigate: (Route) -> Void

    init(dependencies: AppDependencies, navigate: @escaping (Route) -> Void) {
        _viewModel = StateObject(wrappedValue: HomeViewModel(store: dependencies.container.homeStore()))
        self.navigate = navigate
    }

    var body: some View {
        content
            .navigationTitle("Question Bank")
            .toolbar {
                ToolbarItemGroup(placement: .navigationBarTrailing) {
                    Button { navigate(.bookmarks) } label: {
                        Image(systemName: "bookmark")
                    }
                    .accessibilityLabel("Bookmarks")
                    .accessibilityIdentifier("home.bookmarks")

                    Button { navigate(.stats) } label: {
                        Image(systemName: "chart.bar.xaxis")
                    }
                    .accessibilityLabel("Statistics")
                    .accessibilityIdentifier("home.stats")
                }
            }
    }

    @ViewBuilder
    private var content: some View {
        let state = viewModel.state
        if state.isLoading && !state.hasContent {
            ProgressView("Preparing your question bank…")
        } else if let message = state.errorMessage, !state.hasContent {
            EmptyStateView(
                systemImage: "exclamationmark.icloud",
                title: "Couldn't load questions",
                message: message,
                actionTitle: "Try again",
                action: viewModel.retry
            )
        } else {
            List {
                Section {
                    StatsSummaryView(stats: state.stats, totalQuestions: Int(state.totalQuestions))
                    Button {
                        navigate(.quiz(.quick))
                    } label: {
                        Label("Start quick quiz", systemImage: "play.fill")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 6)
                    }
                    .buttonStyle(.borderedProminent)
                    .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                    .accessibilityIdentifier("home.quickQuiz")
                }

                Section("Categories") {
                    ForEach(state.categories, id: \.id) { category in
                        Button {
                            navigate(.quiz(QuizConfig(categoryId: category.id, title: category.title, bookmarkedOnly: false)))
                        } label: {
                            CategoryRow(category: category)
                        }
                        .buttonStyle(PressableButtonStyle())
                        .accessibilityIdentifier("home.category.\(category.id)")
                    }
                }
            }
            .listStyle(.insetGrouped)
            .animation(.default, value: state.categories.count)
        }
    }
}

private struct CategoryRow: View {
    let category: Shared.Category

    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(category.title).font(.headline)
                Text(category.description_)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Text("\(category.questionCount) questions")
                    .font(.caption)
                    .foregroundStyle(.tint)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .foregroundStyle(.tertiary)
        }
        .contentShape(Rectangle())
        .padding(.vertical, 4)
    }
}
