import UIKit
import Shared

protocol HapticsPerforming: AnyObject {
    func perform(_ kind: HapticKind)
}

/// Maps the shared `HapticKind` effects to native feedback generators.
final class SystemHaptics: HapticsPerforming {
    private let notification = UINotificationFeedbackGenerator()
    private let selection = UISelectionFeedbackGenerator()

    func perform(_ kind: HapticKind) {
        switch kind {
        case .correct, .completed:
            notification.notificationOccurred(.success)
        case .incorrect:
            notification.notificationOccurred(.error)
        default:
            selection.selectionChanged()
        }
    }
}

final class SilentHaptics: HapticsPerforming {
    func perform(_ kind: HapticKind) {}
}
