package dev.toufikforyou.questionbank.domain.usecase

import dev.toufikforyou.questionbank.domain.model.AnswerRecord
import dev.toufikforyou.questionbank.domain.model.AnswerResult
import dev.toufikforyou.questionbank.domain.model.DomainError
import dev.toufikforyou.questionbank.domain.model.TimeProvider
import dev.toufikforyou.questionbank.domain.repository.AttemptRepository
import dev.toufikforyou.questionbank.domain.repository.QuestionRepository

class AnswerQuestionUseCase(
    private val questionRepository: QuestionRepository,
    private val attemptRepository: AttemptRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(questionId: String, optionId: String): Result<AnswerResult> = resultOf {
        val question = questionRepository.getQuestion(questionId) ?: throw DomainError.QuestionNotFound(questionId)
        if (!question.hasOption(optionId)) throw DomainError.InvalidOption(questionId, optionId)

        val isCorrect = optionId == question.correctOptionId
        attemptRepository.record(
            AnswerRecord(
                questionId = question.id,
                categoryId = question.categoryId,
                selectedOptionId = optionId,
                isCorrect = isCorrect,
                answeredAtMillis = timeProvider.nowMillis(),
            ),
        )
        AnswerResult(
            questionId = question.id,
            selectedOptionId = optionId,
            correctOptionId = question.correctOptionId,
            isCorrect = isCorrect,
            explanation = question.explanation,
        )
    }
}
