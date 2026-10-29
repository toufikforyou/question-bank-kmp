import SwiftUI
import Shared

struct OptionRow: View {
    let label: String
    let text: String
    let optionState: OptionState
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Text(label)
                    .font(.headline)
                    .frame(width: 32, height: 32)
                    .background(Circle().fill(accent.opacity(0.15)))
                    .foregroundStyle(accent)
                Text(text)
                    .font(.body)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if let icon {
                    Image(systemName: icon)
                        .foregroundStyle(accent)
                        .transition(.scale.combined(with: .opacity))
                }
            }
            .padding(14)
            .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(background))
            .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).strokeBorder(accent.opacity(borderOpacity), lineWidth: 1.5))
            .opacity(optionState == .dimmed ? 0.6 : 1)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressableButtonStyle())
        .animation(.spring(response: 0.3, dampingFraction: 0.7), value: optionState)
        .accessibilityValue(accessibilityValue)
        .accessibilityAddTraits(optionState == .selected ? .isSelected : [])
    }

    private var accent: Color {
        switch optionState {
        case .correct: return .green
        case .incorrect: return .red
        case .selected: return .accentColor
        default: return .secondary
        }
    }

    private var background: Color {
        switch optionState {
        case .correct: return Color.green.opacity(0.14)
        case .incorrect: return Color.red.opacity(0.14)
        case .selected: return Color.accentColor.opacity(0.12)
        default: return Color(.systemBackground)
        }
    }

    private var borderOpacity: Double {
        optionState == .idle || optionState == .dimmed ? 0.35 : 1
    }

    private var icon: String? {
        switch optionState {
        case .correct: return "checkmark.circle.fill"
        case .incorrect: return "xmark.circle.fill"
        default: return nil
        }
    }

    private var accessibilityValue: String {
        switch optionState {
        case .correct: return "Correct answer"
        case .incorrect: return "Incorrect answer"
        case .selected: return "Selected"
        default: return ""
        }
    }
}
