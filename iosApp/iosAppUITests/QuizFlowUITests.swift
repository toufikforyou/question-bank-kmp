import XCTest

/// Black-box UI tests. `-ui-testing` gives each launch a fresh database and silent haptics.
final class QuizFlowUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["-ui-testing"]
        app.launch()
    }

    func testLaunchGoesStraightToHomeWithoutLogin() {
        XCTAssertTrue(app.buttons["home.quickQuiz"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.buttons["home.category.kotlin"].exists)
    }

    func testAnsweringRevealsFeedbackAndNextAdvances() {
        startQuickQuiz()
        XCTAssertEqual(app.staticTexts["quiz.counter"].label, "Question 1 of 10")

        app.buttons["quiz.option.0"].tap()

        XCTAssertTrue(app.staticTexts["quiz.feedback"].waitForExistence(timeout: 5))
        let next = app.buttons["quiz.next"]
        XCTAssertTrue(next.waitForExistence(timeout: 5))
        next.tap()

        XCTAssertTrue(app.staticTexts["Question 2 of 10"].waitForExistence(timeout: 5))
    }

    func testSwipeLeftAdvancesAfterAnswering() {
        startQuickQuiz()
        app.buttons["quiz.option.1"].tap()
        XCTAssertTrue(app.buttons["quiz.next"].waitForExistence(timeout: 5))

        app.scrollViews["quiz.card"].swipeLeft()

        XCTAssertTrue(app.staticTexts["Question 2 of 10"].waitForExistence(timeout: 5))
    }

    func testCompletingQuizShowsResults() {
        startQuickQuiz()

        for _ in 0..<10 {
            app.buttons["quiz.option.0"].tap()
            let next = app.buttons["quiz.next"]
            XCTAssertTrue(next.waitForExistence(timeout: 5))
            next.tap()
        }

        XCTAssertTrue(app.staticTexts["quiz.result.summary"].waitForExistence(timeout: 5))
        app.buttons["quiz.done"].tap()
        XCTAssertTrue(app.buttons["home.quickQuiz"].waitForExistence(timeout: 5))
    }

    func testBookmarkedQuestionAppearsInBookmarks() {
        startQuickQuiz()
        app.buttons["quiz.bookmark"].tap()
        app.navigationBars.buttons.element(boundBy: 0).tap()

        app.buttons["home.bookmarks"].tap()

        XCTAssertTrue(app.buttons["bookmarks.practice"].waitForExistence(timeout: 5))
    }

    private func startQuickQuiz() {
        let start = app.buttons["home.quickQuiz"]
        XCTAssertTrue(start.waitForExistence(timeout: 10))
        start.tap()
        XCTAssertTrue(app.staticTexts["quiz.counter"].waitForExistence(timeout: 5))
    }
}
