import SwiftUI
import Shared

/// One screen for board papers, chapter-wise questions, search and bookmarks.
struct QuestionListView: View {
    @StateObject private var viewModel: QuestionListViewModel
    @State private var query = ""
    private let navigate: (Route) -> Void

    init(store: QuestionListStore, haptics: HapticsPerforming, navigate: @escaping (Route) -> Void) {
        _viewModel = StateObject(wrappedValue: QuestionListViewModel(store: store, haptics: haptics))
        self.navigate = navigate
    }

    var body: some View {
        let state = viewModel.state
        Group {
            if state.isSearch {
                content(state).searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always), prompt: "যেমন: নিউটন, মূলধন")
            } else {
                content(state)
            }
        }
        .navigationTitle(state.title)
        .navigationBarTitleDisplayMode(.inline)
        .onChange(of: query) { viewModel.search($0) }
        .alert("কিছু একটা ভুল হয়েছে", isPresented: Binding(get: { viewModel.message != nil }, set: { if !$0 { viewModel.message = nil } })) {
            Button("ঠিক আছে", role: .cancel) {}
        } message: {
            Text(viewModel.message ?? "")
        }
    }

    @ViewBuilder
    private func content(_ state: QuestionListState) -> some View {
        if state.isLoading {
            ProgressView()
        } else if let message = state.errorMessage {
            EmptyStateView(systemImage: "exclamationmark.triangle", title: "প্রশ্ন পাওয়া যায়নি", message: message,
                           actionTitle: state.isSearch ? nil : "আবার চেষ্টা করুন", action: state.isSearch ? nil : viewModel.retry)
        } else if state.isEmpty {
            EmptyStateView(systemImage: state.isBookmarks ? "bookmark" : "magnifyingglass",
                           title: state.isBookmarks ? "কোনো সংরক্ষিত প্রশ্ন নেই" : "প্রশ্ন খুঁজুন",
                           message: state.emptyMessage)
        } else {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 14) {
                    if !state.subtitle.isEmpty {
                        Text(state.subtitle).font(.subheadline).foregroundStyle(.secondary)
                            .accessibilityIdentifier("list.subtitle")
                    }
                    Picker("ধরন", selection: Binding(get: { state.filter }, set: { viewModel.filter($0) })) {
                        Text("সব (\(Labels.shared.number(value: Int32(state.items.count))))").tag(QuestionFilter.all)
                        Text("বহুনির্বাচনি").tag(QuestionFilter.mcq)
                        Text("সৃজনশীল").tag(QuestionFilter.cq)
                    }
                    .pickerStyle(.segmented)
                    .accessibilityIdentifier("list.filter")

                    if let request = state.practiceRequest {
                        Button { navigate(.quiz(request)) } label: {
                            Label(state.practiceLabel, systemImage: "timer")
                                .font(.headline).frame(maxWidth: .infinity).padding(.vertical, 6)
                        }
                        .buttonStyle(.borderedProminent)
                        .accessibilityIdentifier("list.practise")
                    }

                    if state.isFilteredEmpty {
                        Text(state.filteredEmptyMessage)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 24)
                            .accessibilityIdentifier("list.filteredEmpty")
                    }
                    ForEach(state.visibleItems, id: \.question.id) { item in
                        if item.question.isMcq {
                            McqCard(item: item, state: state, viewModel: viewModel)
                        } else {
                            CreativeCard(item: item, isRevealed: state.isRevealed(questionId: item.question.id), viewModel: viewModel)
                        }
                    }
                }
                .padding()
                .animation(.spring(response: 0.35, dampingFraction: 0.85), value: state.visibleItems.count)
            }
            .background(Color(.systemGroupedBackground))
        }
    }
}

private struct CardHeader: View {
    let item: QuestionItem
    let onBookmark: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Text(item.number)
                .font(.subheadline.weight(.bold))
                .padding(.horizontal, 8).padding(.vertical, 4)
                .background(RoundedRectangle(cornerRadius: 6).fill(Color.accentColor.opacity(0.15)))
            VStack(alignment: .leading, spacing: 1) {
                Text(item.chapterName).font(.caption).foregroundStyle(.secondary)
                if !item.subjectName.isEmpty {
                    Text(item.subjectName).font(.caption2).foregroundStyle(.purple)
                }
            }
            Spacer()
            Button(action: onBookmark) {
                Image(systemName: item.question.isBookmarked ? "bookmark.fill" : "bookmark")
            }
            .accessibilityLabel(item.question.isBookmarked ? "সংরক্ষণ বাতিল" : "সংরক্ষণ করুন")
            .accessibilityIdentifier("question.bookmark.\(item.question.id)")
        }
    }
}

private struct TagRow: View {
    let item: QuestionItem

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                if let repeatLabel = item.repeatLabel {
                    Label(repeatLabel, systemImage: "repeat")
                        .font(.caption.weight(.semibold))
                        .padding(.horizontal, 8).padding(.vertical, 4)
                        .background(Capsule().fill(Color.orange.opacity(0.18)))
                        .foregroundStyle(.orange)
                        .accessibilityIdentifier("question.repeat.\(item.question.id)")
                }
                ForEach(item.tags, id: \.self) { tag in
                    Text(tag)
                        .font(.caption)
                        .padding(.horizontal, 8).padding(.vertical, 4)
                        .background(Capsule().fill(Color(.tertiarySystemFill)))
                }
            }
        }
    }
}

private struct McqCard: View {
    let item: QuestionItem
    let state: QuestionListState
    @ObservedObject var viewModel: QuestionListViewModel

    var body: some View {
        let question = item.question
        let revealed = state.isRevealed(questionId: question.id)
        let answer = state.answers[question.id]
        VStack(alignment: .leading, spacing: 10) {
            CardHeader(item: item) { viewModel.toggleBookmark(question.id) }
            Text(question.prompt).font(.headline)
            ForEach(Array(question.options.enumerated()), id: \.element.id) { index, option in
                OptionRow(
                    label: option.id,
                    text: option.text,
                    optionState: state.optionState(questionId: question.id, optionId: option.id),
                    action: { viewModel.select(questionId: question.id, optionId: option.id) }
                )
                .accessibilityIdentifier("question.option.\(question.id).\(index)")
            }
            if revealed {
                VStack(alignment: .leading, spacing: 3) {
                    if let answer {
                        Text(answer.isCorrect ? "সঠিক উত্তর!" : "ভুল হয়েছে")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(answer.isCorrect ? Color.green : Color.red)
                            .accessibilityIdentifier("question.feedback.\(question.id)")
                    }
                    if let explanation = question.explanation {
                        Text(explanation).font(.subheadline).foregroundStyle(.secondary)
                    }
                }
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
            if answer == nil {
                Button(revealed ? "উত্তর লুকান" : "উত্তর দেখুন") { viewModel.toggleReveal(question.id) }
                    .font(.subheadline)
                    .accessibilityIdentifier("question.reveal.\(question.id)")
            }
            TagRow(item: item)
        }
        .cardStyle()
        .animation(.easeInOut(duration: 0.25), value: revealed)
    }
}

private struct CreativeCard: View {
    let item: QuestionItem
    let isRevealed: Bool
    @ObservedObject var viewModel: QuestionListViewModel

    var body: some View {
        let question = item.question
        VStack(alignment: .leading, spacing: 12) {
            CardHeader(item: item) { viewModel.toggleBookmark(question.id) }
            VStack(alignment: .leading, spacing: 4) {
                Text("উদ্দীপক").font(.caption.weight(.semibold)).foregroundStyle(.tint)
                Text(question.prompt).font(.body)
            }
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(RoundedRectangle(cornerRadius: 12).fill(Color(.tertiarySystemFill)))

            ForEach(question.parts, id: \.label) { part in
                VStack(alignment: .leading, spacing: 6) {
                    HStack(alignment: .firstTextBaseline) {
                        Text("\(part.label).").font(.body.weight(.semibold))
                        Text(part.text).font(.body)
                        Spacer(minLength: 8)
                        Text(Labels.shared.number(value: part.marks)).font(.caption).foregroundStyle(.secondary)
                    }
                    if isRevealed {
                        Text(part.answer)
                            .font(.subheadline)
                            .padding(8)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(RoundedRectangle(cornerRadius: 8).fill(Color.green.opacity(0.12)))
                            .transition(.opacity.combined(with: .move(edge: .top)))
                    }
                }
            }
            Button(isRevealed ? "নমুনা উত্তর লুকান" : "নমুনা উত্তর দেখুন") { viewModel.toggleReveal(question.id) }
                .font(.subheadline)
                .accessibilityIdentifier("question.reveal.\(question.id)")
            TagRow(item: item)
        }
        .cardStyle()
        .animation(.easeInOut(duration: 0.25), value: isRevealed)
    }
}

private extension View {
    func cardStyle() -> some View {
        padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(Color(.secondarySystemGroupedBackground))
                    .shadow(color: .black.opacity(0.06), radius: 8, y: 3)
            )
    }
}
