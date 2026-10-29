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

        XCTAssertTrue(app.buttons["finder.open"].waitForExistence(timeout: 30))
        capture("ios-1-home")

        app.buttons["finder.open"].tap()
        XCTAssertTrue(app.buttons["list.practise"].waitForExistence(timeout: 10))
        capture("ios-2-board-paper")

        app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'question.option.'")).firstMatch.tap()
        XCTAssertTrue(app.staticTexts.matching(NSPredicate(format: "identifier BEGINSWITH 'question.feedback.'")).firstMatch.waitForExistence(timeout: 5))
        capture("ios-3-answer")

        app.buttons["list.practise"].tap()
        XCTAssertTrue(app.staticTexts["quiz.counter"].waitForExistence(timeout: 5))
        capture("ios-4-exam-mode")
        app.navigationBars.buttons.element(boundBy: 0).tap()
        app.navigationBars.buttons.element(boundBy: 0).tap()

        let subject = app.buttons["home.subject.ssc-physics"]
        for _ in 0..<8 where !subject.isHittable { app.swipeUp() }
        subject.tap()
        XCTAssertTrue(app.segmentedControls.firstMatch.waitForExistence(timeout: 5))
        capture("ios-5-subject-papers")

        app.segmentedControls.buttons["অধ্যায়ভিত্তিক"].tap()
        app.buttons["subject.chapter.ssc-physics-2"].tap()
        XCTAssertTrue(app.buttons["list.practise"].waitForExistence(timeout: 10))
        app.segmentedControls.buttons["সৃজনশীল"].tap()
        let reveal = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH 'question.reveal.'")).firstMatch
        if reveal.waitForExistence(timeout: 5) { reveal.tap() }
        capture("ios-6-creative")
    }

    private func capture(_ name: String) {
        sleep(1)
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
