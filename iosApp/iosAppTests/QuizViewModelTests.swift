import XCTest
import Shared
@testable import QuestionBank

/// Verifies that the SwiftUI-facing view model mirrors every Kotlin `StateFlow` emission
/// and routes one-off effects (haptics) to native components.
final class QuizViewModelTests: XCTestCase {

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
    func testStartPublishesInProgressStateWithFirstQuestion() async {
        let viewModel = makeViewModel()

        viewModel.startIfNeeded(.quick)
        await waitForState(viewModel.$state) { $0.status == .inProgress }

        XCTAssertNotNil(viewModel.state.currentQuestion)
        XCTAssertEqual(viewModel.state.questionNumber, 1)
        XCTAssertEqual(viewModel.state.totalQuestions, 10)
        XCTAssertFalse(viewModel.state.isAnswerRevealed)
    }

    @MainActor
    func testCorrectAnswerRevealsAndTriggersSuccessHaptic() async throws {
        let haptics = HapticsSpy()
        let viewModel = makeViewModel(haptics: haptics)
        viewModel.startIfNeeded(.quick)
        await waitForState(viewModel.$state) { $0.status == .inProgress }
        let question = try XCTUnwrap(viewModel.state.currentQuestion)

        viewModel.select(question.correctOptionId)
        await waitForState(viewModel.$state) { $0.isAnswerRevealed }

        XCTAssertEqual(viewModel.state.optionState(optionId: question.correctOptionId), OptionState.correct)
        XCTAssertEqual(viewModel.state.score.correct, 1)
        XCTAssertEqual(haptics.performed.last, HapticKind.correct)
    }

    @MainActor
    func testWrongAnswerMarksSelectionIncorrectAndTriggersErrorHaptic() async throws {
        let haptics = HapticsSpy()
        let viewModel = makeViewModel(haptics: haptics)
        viewModel.startIfNeeded(.quick)
        await waitForState(viewModel.$state) { $0.status == .inProgress }
        let question = try XCTUnwrap(viewModel.state.currentQuestion)
        let wrong = try XCTUnwrap(question.options.first { $0.id != question.correctOptionId })

        viewModel.select(wrong.id)
        await waitForState(viewModel.$state) { $0.isAnswerRevealed }

        XCTAssertEqual(viewModel.state.optionState(optionId: wrong.id), OptionState.incorrect)
        XCTAssertEqual(viewModel.state.optionState(optionId: question.correctOptionId), OptionState.correct)
        XCTAssertEqual(haptics.performed.last, HapticKind.incorrect)
    }

    @MainActor
    func testNextAdvancesToFollowingQuestion() async throws {
        let viewModel = makeViewModel()
        viewModel.startIfNeeded(.quick)
        await waitForState(viewModel.$state) { $0.status == .inProgress }
        let first = try XCTUnwrap(viewModel.state.currentQuestion)

        viewModel.select(first.correctOptionId)
        await waitForState(viewModel.$state) { $0.isAnswerRevealed }
        viewModel.next()

        XCTAssertEqual(viewModel.state.questionNumber, 2)
        XCTAssertNotEqual(viewModel.state.currentQuestion?.id, first.id)
        XCTAssertFalse(viewModel.state.isAnswerRevealed)
    }

    @MainActor
    func testToggleBookmarkUpdatesCurrentQuestion() async throws {
        let viewModel = makeViewModel()
        viewModel.startIfNeeded(.quick)
        await waitForState(viewModel.$state) { $0.status == .inProgress }

        viewModel.toggleBookmark()
        await waitForState(viewModel.$state) { $0.currentQuestion?.isBookmarked == true }

        XCTAssertEqual(viewModel.state.currentQuestion?.isBookmarked, true)
    }

    @MainActor
    func testEmptyBookmarkQuizShowsEmptyState() async {
        let viewModel = makeViewModel()

        viewModel.startIfNeeded(.bookmarks)
        await waitForState(viewModel.$state) { $0.status == .empty }

        XCTAssertNotNil(viewModel.state.errorMessage)
    }
}
