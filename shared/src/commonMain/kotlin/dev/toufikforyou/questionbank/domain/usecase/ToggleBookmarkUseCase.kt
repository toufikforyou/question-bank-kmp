package dev.toufikforyou.questionbank.domain.usecase

import dev.toufikforyou.questionbank.domain.model.DomainError
import dev.toufikforyou.questionbank.domain.model.Question
import dev.toufikforyou.questionbank.domain.repository.QuestionRepository
import kotlinx.coroutines.flow.Flow

/** Flips the bookmark flag and returns the new state. */
class ToggleBookmarkUseCase(
    private val repository: QuestionRepository,
) {
    suspend operator fun invoke(questionId: String): Result<Boolean> = resultOf {
        val question = repository.getQuestion(questionId) ?: throw DomainError.QuestionNotFound(questionId)
        val bookmarked = !question.isBookmarked
        repository.setBookmarked(questionId, bookmarked)
        bookmarked
    }
}

class GetBookmarkedQuestionsUseCase(
    private val repository: QuestionRepository,
) {
    operator fun invoke(): Flow<List<Question>> = repository.observeBookmarkedQuestions()
}
