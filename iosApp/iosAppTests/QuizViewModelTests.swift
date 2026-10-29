import XCTest
import Shared
@testable import QuestionBank

/// Verifies that the SwiftUI-facing view model mirrors every Kotlin `StateFlow` emission
/// and routes one-off effects (haptics) to native components.
final class QuizViewModelTests: XCTestCase {

    private let physics = QuizRequest.Companion.shared.forSubject(subjectId: "ssc-physics")

    @MainActor
    private func makeViewModel(haptics: HapticsSpy = HapticsSpy()) -> QuizViewModel {
        QuizViewModel(store: TestContainers.fresh().quizStore(), haptics: haptics)
    }

    @MainActor
    func testInitialStateIsIdleBeforeStart() {
        let viewModel = makeViewModel()

        XCTAssertEqual(viewModel.state.status, QuizStatus.idle)
        XCTAssertNil(viewModel.state.currentQuestion)
    }

    @MainActor
    func testStartPublishesInProgressStateWithBanglaProgress() async {
        let viewModel = makeViewModel()

        viewModel.startIfNeeded(physics)
        await waitForState(viewModel.$state) { $0.status == .inProgress }

        XCTAssertNotNil(viewModel.state.currentQuestion)
        XCTAssertEqual(viewModel.state.totalQuestions, 10)
        XCTAssertEqual(viewModel.state.progressLabel, "প্রশ্ন ১ / ১০")
        XCTAssertNil(viewModel.state.timerLabel)
    }

    @MainActor
    func testCorrectAnswerRevealsAndTriggersSuccessHaptic() async throws {
        let haptics = HapticsSpy()
        let viewModel = makeViewModel(haptics: haptics)
        viewModel.startIfNeeded(physics)
        await waitForState(viewModel.$state) { $0.status == .inProgress }
        let question = try XCTUnwrap(viewModel.state.currentQuestion)
        let correct = try XCTUnwrap(question.correctOptionId)

        viewModel.select(correct)
        await waitForState(viewModel.$state) { $0.isAnswerRevealed }

        XCTAssertEqual(viewModel.state.optionState(optionId: correct), OptionState.correct)
        XCTAssertEqual(haptics.performed.last, HapticKind.correct)
    }

    @MainActor
    func testWrongAnswerTriggersErrorHaptic() async throws {
        let haptics = HapticsSpy()
        let viewModel = makeViewModel(haptics: haptics)
        viewModel.startIfNeeded(physics)
        await waitForState(viewModel.$state) { $0.status == .inProgress }
        let question = try XCTUnwrap(viewModel.state.currentQuestion)
        let wrong = try XCTUnwrap(question.options.first { $0.id != question.correctOptionId })

        viewModel.select(wrong.id)
        await waitForState(viewModel.$state) { $0.isAnswerRevealed }

        XCTAssertEqual(viewModel.state.optionState(optionId: wrong.id), OptionState.incorrect)
        XCTAssertEqual(haptics.performed.last, HapticKind.incorrect)
    }

    @MainActor
    func testPaperExamModeIsTimedAndKeepsPaperOrder() async {
        let key = PaperKey(subjectId: "ssc-physics", boardId: "dhaka", year: 2023)
        let viewModel = makeViewModel()

        viewModel.startIfNeeded(QuizRequest.Companion.shared.forPaper(paper: key))
        await waitForState(viewModel.$state) { $0.status == .inProgress }

        XCTAssertEqual(viewModel.state.totalQuestions, 6)
        XCTAssertEqual(viewModel.state.timerLabel, "০৬:০০")
    }

    @MainActor
    func testToggleBookmarkUpdatesCurrentQuestion() async {
        let viewModel = makeViewModel()
        viewModel.startIfNeeded(physics)
        await waitForState(viewModel.$state) { $0.status == .inProgress }

        viewModel.toggleBookmark()
        await waitForState(viewModel.$state) { $0.currentQuestion?.isBookmarked == true }
    }

    @MainActor
    func testEmptyBookmarkQuizShowsEmptyState() async {
        let viewModel = makeViewModel()

        viewModel.startIfNeeded(QuizRequest.Companion.shared.forBookmarks())
        await waitForState(viewModel.$state) { $0.status == .empty }

        XCTAssertNotNil(viewModel.state.errorMessage)
    }
}
