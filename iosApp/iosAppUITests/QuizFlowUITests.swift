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

    func testLaunchGoesStraightToHomeWithFinder() {
        XCTAssertTrue(app.buttons["finder.open"].waitForExistence(timeout: 90))
        XCTAssertTrue(app.buttons["home.search"].exists)
    }

    func testFinderOpensPaperAndInlineAnswerShowsFeedback() {
        openDefaultPaper()

        let option = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'question.option.'")).firstMatch
        XCTAssertTrue(option.waitForExistence(timeout: 5))
        option.tap()

        let feedback = app.staticTexts.matching(NSPredicate(format: "identifier BEGINSWITH 'question.feedback.'")).firstMatch
        XCTAssertTrue(feedback.waitForExistence(timeout: 5))
    }

    func testPaperExamModeShowsTimerAndAdvances() {
        openDefaultPaper()
        app.buttons["list.practise"].tap()

        XCTAssertTrue(app.staticTexts["quiz.counter"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["প্রশ্ন ১ / ৬"].exists)
        XCTAssertTrue(app.descendants(matching: .any)["quiz.timer"].exists)

        app.buttons["quiz.option.0"].tap()
        let next = app.buttons["quiz.next"]
        XCTAssertTrue(next.waitForExistence(timeout: 5))
        next.tap()
        XCTAssertTrue(app.staticTexts["প্রশ্ন ২ / ৬"].waitForExistence(timeout: 5))
    }

    func testSubjectChaptersOpenChapterQuestions() {
        let subject = app.buttons["home.subject.ssc-physics"]
        XCTAssertTrue(waitAndScrollTo(subject))
        subject.tap()

        app.segmentedControls.buttons["অধ্যায়ভিত্তিক"].tap()
        let chapter = app.buttons["subject.chapter.ssc-physics-3"]
        XCTAssertTrue(chapter.waitForExistence(timeout: 5))
        chapter.tap()

        XCTAssertTrue(app.buttons["list.practise"].waitForExistence(timeout: 10))
    }

    private func openDefaultPaper() {
        let open = app.buttons["finder.open"]
        XCTAssertTrue(open.waitForExistence(timeout: 90))
        open.tap()
        XCTAssertTrue(app.buttons["list.practise"].waitForExistence(timeout: 10))
    }

    private func waitAndScrollTo(_ element: XCUIElement) -> Bool {
        guard app.buttons["finder.open"].waitForExistence(timeout: 90) else { return false }
        for _ in 0..<8 where !element.isHittable {
            app.swipeUp()
        }
        return element.isHittable
    }
}
