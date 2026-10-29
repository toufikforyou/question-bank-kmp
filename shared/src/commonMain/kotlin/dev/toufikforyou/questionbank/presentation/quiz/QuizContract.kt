package dev.toufikforyou.questionbank.presentation.quiz

import dev.toufikforyou.questionbank.domain.model.AnswerResult
import dev.toufikforyou.questionbank.domain.model.Question
import dev.toufikforyou.questionbank.domain.model.QuizRequest
import dev.toufikforyou.questionbank.domain.model.QuizScore
import dev.toufikforyou.questionbank.domain.model.QuizSession
import dev.toufikforyou.questionbank.presentation.format.Labels

enum class QuizStatus { IDLE, LOADING, IN_PROGRESS, FINISHED, EMPTY, ERROR }

/** Visual state of a single option, computed once here so both UIs render identically. */
enum class OptionState { IDLE, SELECTED, CORRECT, INCORRECT, DIMMED }

/** Platform-neutral haptic intents; Android/iOS map them to native feedback generators. */
enum class HapticKind { SELECTION, CORRECT, INCORRECT, COMPLETED }

data class QuizState(
    val status: QuizStatus = QuizStatus.IDLE,
    val session: QuizSession? = null,
    val selectedOptionId: String? = null,
    val lastAnswer: AnswerResult? = null,
    /** Seconds left in exam mode; null when the session is untimed. */
    val remainingSeconds: Int? = null,
    val timeUp: Boolean = false,
    val errorMessage: String? = null,
) {
    val currentQuestion: Question?
        get() = if (status == QuizStatus.IN_PROGRESS) session?.currentQuestion else null

    val isAnswerRevealed: Boolean get() = lastAnswer != null
    val isAwaitingAnswer: Boolean get() = selectedOptionId != null && lastAnswer == null
    val isLastQuestion: Boolean get() = session?.isLastQuestion == true
    val questionNumber: Int get() = (session?.currentIndex ?: 0) + 1
    val totalQuestions: Int get() = session?.questions?.size ?: 0
    val score: QuizScore get() = session?.score ?: QuizScore(0, 0)
    val isTimed: Boolean get() = remainingSeconds != null

    val progress: Float
        get() = if (totalQuestions == 0) 0f else (session?.answeredCount ?: 0).toFloat() / totalQuestions

    val progressLabel: String get() = Labels.progress(questionNumber, totalQuestions)
    val timerLabel: String? get() = remainingSeconds?.let(Labels::clock)
    val isTimeRunningOut: Boolean get() = (remainingSeconds ?: Int.MAX_VALUE) <= 60
    val scoreLabel: String get() = "${Labels.number(totalQuestions)}টির মধ্যে ${Labels.number(score.correct)}টি সঠিক"
    val percentLabel: String get() = Labels.percent(score.percent)

    fun optionState(optionId: String): OptionState {
        val answer = lastAnswer ?: return if (optionId == selectedOptionId) OptionState.SELECTED else OptionState.IDLE
        return when (optionId) {
            answer.correctOptionId -> OptionState.CORRECT
            answer.selectedOptionId -> OptionState.INCORRECT
            else -> OptionState.DIMMED
        }
    }
}

sealed class QuizIntent {
    data class Start(val request: QuizRequest) : QuizIntent()
    data class SelectOption(val optionId: String) : QuizIntent()
    data object Next : QuizIntent()
    data object ToggleBookmark : QuizIntent()
    data object Restart : QuizIntent()
}

sealed class QuizEffect {
    data class Haptic(val kind: HapticKind) : QuizEffect()
    data class ShowMessage(val message: String) : QuizEffect()
}
