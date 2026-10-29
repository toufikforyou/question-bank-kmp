import SwiftUI
import Shared

struct QuizResultView: View {
    let state: QuizState
    let onRestart: () -> Void
    let onDone: () -> Void

    @State private var animatedFraction: Double = 0

    var body: some View {
        VStack(spacing: 20) {
            Spacer()
            if state.timeUp {
                Text("সময় শেষ!").font(.headline).foregroundStyle(.red)
            }
            ZStack {
                Circle().stroke(Color(.systemGray5), lineWidth: 14)
                Circle()
                    .trim(from: 0, to: animatedFraction)
                    .stroke(Color.accentColor, style: StrokeStyle(lineWidth: 14, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                Text(state.percentLabel)
                    .font(.system(size: 44, weight: .bold, design: .rounded))
                    .accessibilityIdentifier("quiz.result.percent")
            }
            .frame(width: 170, height: 170)

            Text(state.score.isPassing ? "চমৎকার!" : "আরও অনুশীলন করুন")
                .font(.title2.weight(.semibold))
            Text(state.scoreLabel)
                .foregroundStyle(.secondary)
                .accessibilityIdentifier("quiz.result.summary")
            Spacer()
            Button(action: onRestart) {
                Text("আবার দিন").frame(maxWidth: .infinity).padding(.vertical, 6)
            }
            .buttonStyle(.borderedProminent)
            .accessibilityIdentifier("quiz.restart")
            Button(action: onDone) {
                Text("ফিরে যান").frame(maxWidth: .infinity).padding(.vertical, 6)
            }
            .buttonStyle(.bordered)
            .accessibilityIdentifier("quiz.done")
        }
        .padding(24)
        .onAppear {
            withAnimation(.easeOut(duration: 0.9)) {
                animatedFraction = Double(state.score.percent) / 100
            }
        }
    }
}
