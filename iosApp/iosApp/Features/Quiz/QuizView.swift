import SwiftUI
import Shared

struct QuizView: View {
    @StateObject private var viewModel: QuizViewModel
    private let request: QuizRequest
    private let onExit: () -> Void

    init(dependencies: AppDependencies, request: QuizRequest, onExit: @escaping () -> Void) {
        _viewModel = StateObject(wrappedValue: QuizViewModel(store: dependencies.container.quizStore(), haptics: dependencies.haptics))
        self.request = request
        self.onExit = onExit
    }

    var body: some View {
        content
            .navigationTitle(request.timed ? "পরীক্ষা" : "অনুশীলন")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItemGroup(placement: .navigationBarTrailing) {
                    if let clock = viewModel.state.timerLabel, viewModel.state.status == .inProgress {
                        Label(clock, systemImage: "timer")
                            .labelStyle(.titleAndIcon)
                            .font(.subheadline.monospacedDigit().weight(.semibold))
                            .foregroundStyle(viewModel.state.isTimeRunningOut ? Color.red : Color.primary)
                            .accessibilityIdentifier("quiz.timer")
                    }
                    if let question = viewModel.state.currentQuestion {
                        Button(action: viewModel.toggleBookmark) {
                            Image(systemName: question.isBookmarked ? "bookmark.fill" : "bookmark")
                        }
                        .accessibilityLabel(question.isBookmarked ? "সংরক্ষণ বাতিল" : "সংরক্ষণ করুন")
                        .accessibilityIdentifier("quiz.bookmark")
                    }
                }
            }
            .task { viewModel.startIfNeeded(request) }
            .alert("কিছু একটা ভুল হয়েছে", isPresented: Binding(get: { viewModel.message != nil }, set: { if !$0 { viewModel.message = nil } })) {
                Button("ঠিক আছে", role: .cancel) {}
            } message: {
                Text(viewModel.message ?? "")
            }
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.state.status {
        case .inProgress:
            QuizQuestionView(viewModel: viewModel)
        case .finished:
            QuizResultView(state: viewModel.state, onRestart: viewModel.restart, onDone: onExit)
        case .empty:
            EmptyStateView(systemImage: "tray", title: "অনুশীলনের প্রশ্ন নেই",
                           message: viewModel.state.errorMessage ?? "এখানে কোনো বহুনির্বাচনি প্রশ্ন নেই।",
                           actionTitle: "ফিরে যান", action: onExit)
        case .error:
            EmptyStateView(systemImage: "exclamationmark.triangle", title: "কিছু একটা ভুল হয়েছে",
                           message: viewModel.state.errorMessage ?? "আবার চেষ্টা করুন।",
                           actionTitle: "আবার চেষ্টা করুন", action: viewModel.restart)
        default:
            ProgressView()
        }
    }
}

/// The active question. Observes the parent's view model rather than owning it.
struct QuizQuestionView: View {
    @ObservedObject var viewModel: QuizViewModel
    @GestureState private var dragOffset: CGFloat = 0

    var body: some View {
        let state = viewModel.state
        VStack(alignment: .leading, spacing: 16) {
            ProgressView(value: Double(state.progress))
                .tint(.accentColor)
                .accessibilityIdentifier("quiz.progress")

            Text(state.progressLabel)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .accessibilityIdentifier("quiz.counter")

            if let question = state.currentQuestion {
                ScrollView {
                    QuestionCard(question: question, state: state, onSelect: viewModel.select)
                        .onTapGesture(count: 2, perform: viewModel.toggleBookmark)
                }
                .id(question.id)
                .transition(.asymmetric(
                    insertion: .move(edge: .trailing).combined(with: .opacity),
                    removal: .move(edge: .leading).combined(with: .opacity)
                ))
                .offset(x: dragOffset)
                .simultaneousGesture(swipeToAdvance)
                .accessibilityIdentifier("quiz.card")
            }

            if state.isAnswerRevealed {
                Button(action: viewModel.next) {
                    Text(state.isLastQuestion ? "ফলাফল দেখুন" : "পরের প্রশ্ন")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 6)
                }
                .buttonStyle(.borderedProminent)
                .accessibilityIdentifier("quiz.next")
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .padding()
        .frame(maxHeight: .infinity, alignment: .top)
        .animation(.spring(response: 0.45, dampingFraction: 0.85), value: state.questionNumber)
        .animation(.easeInOut(duration: 0.2), value: state.isAnswerRevealed)
    }

    /// Swipe left on a revealed card to move on, with a rubber-band preview while dragging.
    private var swipeToAdvance: some Gesture {
        DragGesture(minimumDistance: 24)
            .updating($dragOffset) { value, offset, _ in
                guard viewModel.state.isAnswerRevealed, value.translation.width < 0 else { return }
                offset = value.translation.width / 3
            }
            .onEnded { value in
                if viewModel.state.isAnswerRevealed && value.translation.width < -80 {
                    viewModel.next()
                }
            }
    }
}

private struct QuestionCard: View {
    let question: Question
    let state: QuizState
    let onSelect: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(question.prompt)
                .font(.title3.weight(.semibold))
                .accessibilityAddTraits(.isHeader)

            ForEach(Array(question.options.enumerated()), id: \.element.id) { index, option in
                OptionRow(
                    label: option.id,
                    text: option.text,
                    optionState: state.optionState(optionId: option.id),
                    action: { onSelect(option.id) }
                )
                .accessibilityIdentifier("quiz.option.\(index)")
            }

            if let answer = state.lastAnswer {
                VStack(alignment: .leading, spacing: 4) {
                    Text(answer.isCorrect ? "সঠিক উত্তর!" : "ভুল হয়েছে")
                        .font(.headline)
                        .foregroundStyle(answer.isCorrect ? Color.green : Color.red)
                        .accessibilityIdentifier("quiz.feedback")
                    if let explanation = answer.explanation {
                        Text(explanation).font(.subheadline).foregroundStyle(.secondary)
                    }
                }
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .padding(20)
        .background(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(Color(.secondarySystemGroupedBackground))
                .shadow(color: .black.opacity(0.08), radius: 12, y: 4)
        )
        .padding(.vertical, 4)
        .animation(.easeInOut(duration: 0.25), value: state.isAnswerRevealed)
    }
}
