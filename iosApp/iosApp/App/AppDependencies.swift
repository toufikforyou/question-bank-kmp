import Foundation
import Shared

enum LaunchArguments {
    /// Starts the app with a fresh, isolated database and silent haptics (used by XCUITest).
    static let uiTesting = "-ui-testing"
}

/// Composition root for the iOS app. The Kotlin `AppContainer` owns repositories and caches.
final class AppDependencies {
    static let shared = AppDependencies(arguments: ProcessInfo.processInfo.arguments)

    let container: AppContainer
    let haptics: HapticsPerforming

    init(arguments: [String]) {
        let isUITesting = arguments.contains(LaunchArguments.uiTesting)
        let databaseName = isUITesting
            ? "questionbank-uitest-\(UUID().uuidString).db"
            : IosAppContainerFactory.shared.DEFAULT_DATABASE_NAME
        container = IosAppContainerFactory.shared.create(databaseName: databaseName)
        haptics = isUITesting ? SilentHaptics() : SystemHaptics()
    }

    init(container: AppContainer, haptics: HapticsPerforming) {
        self.container = container
        self.haptics = haptics
    }
}
