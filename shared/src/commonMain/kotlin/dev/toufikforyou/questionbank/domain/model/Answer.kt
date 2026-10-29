package dev.toufikforyou.questionbank.domain.model

/** A persisted answer attempt. */
data class AnswerRecord(
    val questionId: String,
    val categoryId: String,
    val selectedOptionId: String,
    val isCorrect: Boolean,
    val answeredAtMillis: Long,
)

/** The outcome of answering a question, returned to the presentation layer. */
data class AnswerResult(
    val questionId: String,
    val selectedOptionId: String,
    val correctOptionId: String,
    val isCorrect: Boolean,
    val explanation: String?,
)
