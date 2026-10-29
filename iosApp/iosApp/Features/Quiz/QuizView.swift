import SwiftUI
import Shared

struct QuizView: View {
    @StateObject private var viewModel: QuizViewModel
    private let config: QuizConfig
    private let onExit: () -> Void

    init(dependencies: AppDependencies, config: QuizConfig, onExit: @escaping () -> Void) {
        _viewModel = StateObject(
            wrappedValue: QuizViewModel(store: dependencies.container.quizStore(), haptics: dependencies.haptics)
        )
        self.config = config
        self.onExit = onExit
    }

    var body: some View {
        content
            .navigationTitle(config.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    if let question = viewModel.state.currentQuestion {
                        Button(action: viewModel.toggleBookmark) {
                            Image(systemName: question.isBookmarked ? "bookmark.fill" : "bookmark")
                        }
                        .accessibilityLabel(question.isBookmarked ? "Remove bookmark" : "Bookmark question")
                        .accessibilityIdentifier("quiz.bookmark")
                    }
                }
            }
            .task { viewModel.startIfNeeded(config) }
            .alert(
                "Something went wrong",
                isPresented: Binding(get: { viewModel.message != nil }, set: { if !$0 { viewModel.message = nil } })
            ) {
                Button("OK", role: .cancel) {}
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
            QuizResultView(score: viewModel.state.score, onRestart: viewModel.restart, onDone: onExit)
        case .empty:
            EmptyStateView(
                systemImage: "tray",
                title: "Nothing to practise yet",
                message: viewModel.state.errorMessage ?? "There are no questions here.",
                actionTitle: "Back to home",
                action: onExit
            )
        case .error:
            EmptyStateView(
                systemImage: "exclamationmark.triangle",
                title: "Something went wrong",
                message: viewModel.state.errorMessage ?? "Please try again.",
                actionTitle: "Retry",
                action: viewModel.restart
            )
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

            Text("Question \(state.questionNumber) of \(state.totalQuestions)")
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
                    Text(state.isLastQuestion ? "See results" : "Next question")
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
            Text(question.difficulty.name.capitalized)
                .font(.caption.weight(.semibold))
                .foregroundStyle(.purple)
            Text(question.prompt)
                .font(.title3.weight(.semibold))
                .accessibilityAddTraits(.isHeader)

            ForEach(Array(question.options.enumerated()), id: \.element.id) { index, option in
                OptionRow(
                    label: String(UnicodeScalar(UInt8(65 + index))),
                    text: option.text,
                    optionState: state.optionState(optionId: option.id),
                    action: { onSelect(option.id) }
                )
                .accessibilityIdentifier("quiz.option.\(index)")
            }

            if let answer = state.lastAnswer {
                VStack(alignment: .leading, spacing: 4) {
                    Text(answer.isCorrect ? "Correct!" : "Not quite")
                        .font(.headline)
                        .foregroundStyle(answer.isCorrect ? Color.green : Color.red)
                        .accessibilityIdentifier("quiz.feedback")
                    if let explanation = answer.explanation {
                        Text(explanation)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
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
