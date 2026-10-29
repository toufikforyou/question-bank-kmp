import SwiftUI
import Shared

struct StatsView: View {
    @StateObject private var viewModel: StatsViewModel

    init(dependencies: AppDependencies) {
        _viewModel = StateObject(wrappedValue: StatsViewModel(store: dependencies.container.statsStore()))
    }

    var body: some View {
        let stats = viewModel.state.stats
        List {
            Section("Overview") {
                LabeledContent("Accuracy", value: "\(stats.accuracyPercent)%")
                LabeledContent("Answered", value: "\(stats.totalAnswered)")
                LabeledContent("Correct", value: "\(stats.totalCorrect)")
                LabeledContent("Current streak", value: "\(stats.currentStreak)")
                LabeledContent("Best streak", value: "\(stats.bestStreak)")
                LabeledContent("Bookmarked", value: "\(stats.bookmarkedCount)")
            }
            Section("By category") {
                ForEach(stats.categoryStats, id: \.categoryId) { category in
                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            Text(category.title).font(.headline)
                            Spacer()
                            Text("\(category.accuracyPercent)%").foregroundStyle(.tint)
                        }
                        ProgressView(value: Double(category.accuracyPercent), total: 100)
                        Text("\(category.correct) / \(category.answered) correct")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 4)
                }
            }
        }
        .animation(.easeInOut, value: stats.totalAnswered)
        .navigationTitle("Statistics")
        .overlay {
            if viewModel.state.isLoading { ProgressView() }
        }
    }
}
