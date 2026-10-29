package dev.toufikforyou.questionbank.domain.usecase

import dev.toufikforyou.questionbank.domain.model.DomainError
import dev.toufikforyou.questionbank.domain.model.Question
import dev.toufikforyou.questionbank.domain.model.QuizRequest
import dev.toufikforyou.questionbank.domain.repository.QuestionRepository
import kotlinx.coroutines.flow.first
import kotlin.random.Random

/**
 * Builds the question list for a quiz. Stateless and therefore safe to share across coroutines.
 */
class GetQuestionsUseCase(
    private val repository: QuestionRepository,
    private val random: Random = Random.Default,
) {
    suspend operator fun invoke(request: QuizRequest = QuizRequest()): Result<List<Question>> = resultOf {
        if (request.limit !in 1..QuizRequest.MAX_LIMIT) throw DomainError.InvalidQuizSize(request.limit)

        val pool = if (request.bookmarkedOnly) {
            repository.observeBookmarkedQuestions().first()
                .filter { request.categoryId == null || it.categoryId == request.categoryId }
        } else {
            repository.getQuestions(request.categoryId)
        }
        if (pool.isEmpty()) throw DomainError.EmptyQuestionBank(request.categoryId)

        val ordered = if (request.shuffle) pool.shuffled(random) else pool
        ordered.take(request.limit)
    }
}
