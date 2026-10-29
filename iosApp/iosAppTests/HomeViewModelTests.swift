import XCTest
import Shared
@testable import QuestionBank

final class HomeViewModelTests: XCTestCase {

    @MainActor
    func testFirstLaunchSeedsBankAndCreatesLocalProfileWithoutLogin() async {
        let viewModel = HomeViewModel(store: TestContainers.fresh().homeStore())
        XCTAssertTrue(viewModel.state.isLoading)

        await waitForState(viewModel.$state) { !$0.isLoading }

        XCTAssertNil(viewModel.state.errorMessage)
        XCTAssertEqual(viewModel.state.profileName, "Learner")
        XCTAssertEqual(viewModel.state.categories.count, 4)
        XCTAssertEqual(viewModel.state.totalQuestions, 24)
        XCTAssertEqual(viewModel.state.stats.totalAnswered, 0)
    }

    @MainActor
    func testStatsUpdateAfterAnsweringInAnotherStore() async throws {
        let container = TestContainers.fresh()
        let home = HomeViewModel(store: container.homeStore())
        await waitForState(home.$state) { !$0.isLoading }

        let quiz = QuizViewModel(store: container.quizStore(), haptics: HapticsSpy())
        quiz.startIfNeeded(.quick)
        await waitForState(quiz.$state) { $0.status == .inProgress }
        let question = try XCTUnwrap(quiz.state.currentQuestion)
        quiz.select(question.correctOptionId)

        await waitForState(home.$state) { $0.stats.totalAnswered == 1 }
        XCTAssertEqual(home.state.stats.accuracyPercent, 100)
    }
}
