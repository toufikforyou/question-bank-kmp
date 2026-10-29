import SwiftUI
import Shared

struct QuizResultView: View {
    let score: QuizScore
    let onRestart: () -> Void
    let onDone: () -> Void

    @State private var animatedFraction: Double = 0

    var body: some View {
        VStack(spacing: 20) {
            Spacer()
            ZStack {
                Circle().stroke(Color(.systemGray5), lineWidth: 14)
                Circle()
                    .trim(from: 0, to: animatedFraction)
                    .stroke(Color.accentColor, style: StrokeStyle(lineWidth: 14, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                Text("\(score.percent)%")
                    .font(.system(size: 44, weight: .bold, design: .rounded))
                    .accessibilityIdentifier("quiz.result.percent")
            }
            .frame(width: 170, height: 170)

            Text(score.isPassing ? "Great work!" : "Keep practising")
                .font(.title2.weight(.semibold))
            Text("You answered \(score.correct) of \(score.total) correctly.")
                .foregroundStyle(.secondary)
                .accessibilityIdentifier("quiz.result.summary")
            Spacer()
            Button(action: onRestart) {
                Text("Try again").frame(maxWidth: .infinity).padding(.vertical, 6)
            }
            .buttonStyle(.borderedProminent)
            .accessibilityIdentifier("quiz.restart")
            Button(action: onDone) {
                Text("Back to home").frame(maxWidth: .infinity).padding(.vertical, 6)
            }
            .buttonStyle(.bordered)
            .accessibilityIdentifier("quiz.done")
        }
        .padding(24)
        .onAppear {
            withAnimation(.easeOut(duration: 0.9)) {
                animatedFraction = Double(score.percent) / 100
            }
        }
    }
}
