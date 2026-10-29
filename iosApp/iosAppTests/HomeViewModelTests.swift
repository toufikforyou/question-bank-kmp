import XCTest
import Shared
@testable import QuestionBank

final class HomeViewModelTests: XCTestCase {

    @MainActor
    func testFirstLaunchSeedsBankAndPrefillsFinderWithoutLogin() async throws {
        let viewModel = HomeViewModel(store: TestContainers.fresh().homeStore())
        XCTAssertTrue(viewModel.state.isLoading)

        await waitForState(viewModel.$state, timeout: 20) { !$0.isLoading }

        let state = viewModel.state
        XCTAssertNil(state.errorMessage)
        XCTAssertTrue(state.isSample)
        XCTAssertEqual(state.boards.count, 11)
        XCTAssertEqual(state.subjects.count, 7)
        XCTAssertEqual(state.selectedExam, ExamType.ssc)
        let key = try XCTUnwrap(state.finderKey)
        XCTAssertEqual(key.boardId, "dhaka")
        XCTAssertEqual(key.year, 2025)
    }

    @MainActor
    func testSwitchingToHscShowsHscSubjects() async {
        let viewModel = HomeViewModel(store: TestContainers.fresh().homeStore())
        await waitForState(viewModel.$state, timeout: 20) { !$0.isLoading }

        viewModel.selectExam(.hsc)
        await waitForState(viewModel.$state) { $0.selectedExam == .hsc }

        XCTAssertTrue(viewModel.state.subjects.allSatisfy { $0.exam == .hsc })
        XCTAssertEqual(viewModel.state.examLabel, "এইচএসসি")
    }
}
