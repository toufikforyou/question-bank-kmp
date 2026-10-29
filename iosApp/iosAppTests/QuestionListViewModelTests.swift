import XCTest
import Shared
@testable import QuestionBank

final class QuestionListViewModelTests: XCTestCase {

    @MainActor
    func testPaperListsAllQuestionsWithBoardTitle() async {
        let key = PaperKey(subjectId: "ssc-physics", boardId: "dhaka", year: 2023)
        let viewModel = QuestionListViewModel(store: TestContainers.fresh().paperStore(key: key), haptics: HapticsSpy())

        await waitForState(viewModel.$state, timeout: 20) { !$0.isLoading }

        XCTAssertEqual(viewModel.state.title, "ঢাকা বোর্ড ২০২৩")
        XCTAssertEqual(viewModel.state.items.count, 8)
        XCTAssertEqual(viewModel.state.mcqCount, 6)
        XCTAssertEqual(viewModel.state.items.first?.number, "১")
        XCTAssertNotNil(viewModel.state.practiceRequest)
    }

    @MainActor
    func testAnsweringInlineRevealsAndFiresHaptic() async throws {
        let haptics = HapticsSpy()
        let key = PaperKey(subjectId: "ssc-math", boardId: "rajshahi", year: 2019)
        let viewModel = QuestionListViewModel(store: TestContainers.fresh().paperStore(key: key), haptics: haptics)
        await waitForState(viewModel.$state, timeout: 20) { !$0.isLoading }
        let question = try XCTUnwrap(viewModel.state.items.first { $0.question.isMcq }?.question)

        viewModel.select(questionId: question.id, optionId: try XCTUnwrap(question.correctOptionId))
        await waitForState(viewModel.$state) { $0.isRevealed(questionId: question.id) }

        XCTAssertEqual(haptics.performed.last, HapticKind.correct)
    }

    @MainActor
    func testSearchFindsMatchingQuestions() async {
        let viewModel = QuestionListViewModel(store: TestContainers.fresh().searchStore(exam: .ssc), haptics: HapticsSpy())

        viewModel.search("বর্গমূল")
        await waitForState(viewModel.$state, timeout: 20) { !$0.items.isEmpty }

        XCTAssertTrue(viewModel.state.items.allSatisfy { $0.question.subjectId.hasPrefix("ssc-") })
    }
}
