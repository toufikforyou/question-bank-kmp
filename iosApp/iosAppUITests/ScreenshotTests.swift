import XCTest

/// Captures store-style screenshots. Runs only when `TEST_RUNNER_CAPTURE_SCREENSHOTS=1` is set
/// (see .github/workflows/screenshots.yml); images are kept as test attachments.
final class ScreenshotTests: XCTestCase {

    override func setUpWithError() throws {
        try XCTSkipUnless(ProcessInfo.processInfo.environment["CAPTURE_SCREENSHOTS"] == "1")
        continueAfterFailure = false
    }

    func testCaptureMainScreens() {
        let app = XCUIApplication()
        app.launchArguments = ["-ui-testing"]
        app.launch()

        XCTAssertTrue(app.buttons["home.quickQuiz"].waitForExistence(timeout: 15))
        capture("ios-1-home")

        app.buttons["home.category.kotlin"].tap()
        XCTAssertTrue(app.staticTexts["quiz.counter"].waitForExistence(timeout: 5))
        capture("ios-2-question")

        app.buttons["quiz.bookmark"].tap()
        app.buttons["quiz.option.1"].tap()
        XCTAssertTrue(app.buttons["quiz.next"].waitForExistence(timeout: 5))
        capture("ios-3-answer")

        app.buttons["quiz.next"].tap()
        for _ in 0..<5 {
            app.buttons["quiz.option.1"].tap()
            XCTAssertTrue(app.buttons["quiz.next"].waitForExistence(timeout: 5))
            app.buttons["quiz.next"].tap()
        }
        XCTAssertTrue(app.staticTexts["quiz.result.summary"].waitForExistence(timeout: 5))
        capture("ios-4-results")

        app.buttons["quiz.done"].tap()
        XCTAssertTrue(app.buttons["home.stats"].waitForExistence(timeout: 5))
        app.buttons["home.stats"].tap()
        XCTAssertTrue(app.navigationBars["Statistics"].waitForExistence(timeout: 5))
        capture("ios-5-statistics")

        app.navigationBars.buttons.element(boundBy: 0).tap()
        app.buttons["home.bookmarks"].tap()
        XCTAssertTrue(app.buttons["bookmarks.practice"].waitForExistence(timeout: 5))
        capture("ios-6-bookmarks")
    }

    private func capture(_ name: String) {
        sleep(1)
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
