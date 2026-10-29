import SwiftUI
import Shared

struct StatsView: View {
    @StateObject private var viewModel: StatsViewModel

    init(dependencies: AppDependencies) {
        _viewModel = StateObject(wrappedValue: StatsViewModel(store: dependencies.container.statsStore()))
    }

    var body: some View {
        let stats = viewModel.state.stats
        let n = { (value: Int32) in Labels.shared.number(value: value) }
        List {
            Section("সারসংক্ষেপ") {
                LabeledContent("সঠিকতা", value: Labels.shared.percent(value: stats.accuracyPercent))
                LabeledContent("উত্তর দিয়েছেন", value: n(stats.totalAnswered))
                LabeledContent("সঠিক উত্তর", value: n(stats.totalCorrect))
                LabeledContent("চলতি ধারা", value: n(stats.currentStreak))
                LabeledContent("সেরা ধারা", value: n(stats.bestStreak))
                LabeledContent("সংরক্ষিত প্রশ্ন", value: n(stats.bookmarkedCount))
            }
            Section("বিষয়ভিত্তিক") {
                if stats.subjectStats.isEmpty {
                    Text("প্রশ্নের উত্তর দিলে এখানে বিষয়ভিত্তিক অগ্রগতি দেখা যাবে।")
                        .font(.subheadline).foregroundStyle(.secondary)
                }
                ForEach(stats.subjectStats, id: \.subjectId) { subject in
                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            Text("\(Labels.shared.exam(type: subject.exam)) · \(subject.title)").font(.headline)
                            Spacer()
                            Text(Labels.shared.percent(value: subject.accuracyPercent)).foregroundStyle(.tint)
                        }
                        ProgressView(value: Double(subject.accuracyPercent), total: 100)
                        Text("\(n(subject.answered))টির মধ্যে \(n(subject.correct))টি সঠিক")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 4)
                }
            }
        }
        .animation(.easeInOut, value: stats.totalAnswered)
        .navigationTitle("পরিসংখ্যান")
        .overlay {
            if viewModel.state.isLoading { ProgressView() }
        }
    }
}
