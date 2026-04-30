import XCTest

final class SafewordsSnapshotTests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testAppStoreScreenshotSmokePath() throws {
        let app = XCUIApplication()
        app.launchArguments += ["-safewordsSnapshotMode"]
        app.launch()

        capture("01-home", app: app)
    }

    private func capture(_ name: String, app: XCUIApplication) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
