package dev.toufikforyou.questionbank.domain.model

import kotlin.math.roundToInt

data class QuizScore(
    val correct: Int,
    val total: Int,
) {
    init {
        require(total >= 0) { "total must not be negative" }
        require(correct in 0..total) { "correct ($correct) must be within 0..$total" }
    }

    val percent: Int get() = percentOf(correct, total)
    val isPassing: Boolean get() = total > 0 && percent >= PASS_MARK_PERCENT

    companion object {
        const val PASS_MARK_PERCENT = 60
    }
}

/** Immutable snapshot of an in-flight quiz. Every mutation returns a new instance. */
data class QuizSession(
    val questions: List<Question>,
    val currentIndex: Int = 0,
    val answers: Map<String, AnswerResult> = emptyMap(),
) {
    init {
        require(currentIndex in 0..questions.size) { "currentIndex $currentIndex out of bounds" }
    }

    val currentQuestion: Question? get() = questions.getOrNull(currentIndex)
    val isFinished: Boolean get() = currentIndex >= questions.size
    val isLastQuestion: Boolean get() = currentIndex == questions.lastIndex
    val answeredCount: Int get() = answers.size
    val score: QuizScore get() = QuizScore(correct = answers.values.count { it.isCorrect }, total = questions.size)

    fun record(result: AnswerResult): QuizSession {
        require(questions.any { it.id == result.questionId }) { "Question ${result.questionId} is not part of this session" }
        return copy(answers = answers + (result.questionId to result))
    }

    fun advance(): QuizSession = if (isFinished) this else copy(currentIndex = currentIndex + 1)

    fun updateQuestion(questionId: String, transform: (Question) -> Question): QuizSession =
        copy(questions = questions.map { if (it.id == questionId) transform(it) else it })
}

internal fun percentOf(part: Int, whole: Int): Int =
    if (whole <= 0) 0 else (part * 100.0 / whole).roundToInt()
