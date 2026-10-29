import Combine
import XCTest
import Shared
@testable import QuestionBank

final class HapticsSpy: HapticsPerforming {
    private(set) var performed: [HapticKind] = []

    func perform(_ kind: HapticKind) {
        performed.append(kind)
    }
}

enum TestContainers {
    /// A real Kotlin container backed by a throwaway on-disk SQLite file and the bundled seed.
    static func fresh() -> AppContainer {
        IosAppContainerFactory.shared.create(databaseName: "unit-\(UUID().uuidString).db")
    }
}

extension XCTestCase {
    /// Waits until a `@Published` state emitted by a Kotlin store satisfies `predicate`.
    @MainActor
    func waitForState<S>(
        _ publisher: Published<S>.Publisher,
        timeout: TimeInterval = 30,
        description: String = "state predicate",
        _ predicate: @escaping (S) -> Bool
    ) async {
        let expectation = expectation(description: description)
        expectation.assertForOverFulfill = false
        let cancellable = publisher.sink { if predicate($0) { expectation.fulfill() } }
        await fulfillment(of: [expectation], timeout: timeout)
        cancellable.cancel()
    }
}
