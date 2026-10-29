import SwiftUI

@main
struct QuestionBankApp: App {
    private let dependencies = AppDependencies.shared

    var body: some Scene {
        WindowGroup {
            RootView(dependencies: dependencies)
        }
    }
}
