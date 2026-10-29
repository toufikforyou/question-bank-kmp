package dev.toufikforyou.questionbank.domain.usecase

import dev.toufikforyou.questionbank.domain.model.DomainError
import dev.toufikforyou.questionbank.domain.model.Question
import dev.toufikforyou.questionbank.domain.model.QuizRequest
import dev.toufikforyou.questionbank.domain.repository.QuestionRepository
import kotlinx.coroutines.flow.first
import kotlin.random.Random

/**
 * Builds the MCQ list for a practice or exam session. Stateless and therefore safe to share across coroutines.
 */
class GetQuestionsUseCase(
    private val repository: QuestionRepository,
    private val random: Random = Random.Default,
) {
    suspend operator fun invoke(request: QuizRequest = QuizRequest()): Result<List<Question>> = resultOf {
        if (request.limit !in 1..QuizRequest.MAX_LIMIT) throw DomainError.InvalidQuizSize(request.limit)

        val source = when {
            request.paper != null -> repository.getPaperQuestions(request.paper)
            request.bookmarkedOnly -> repository.observeBookmarkedQuestions().first()
            request.chapterId != null -> repository.getChapterQuestions(request.chapterId)
            request.subjectId != null -> repository.getSubjectQuestions(request.subjectId)
            else -> throw DomainError.EmptyQuestionBank()
        }
        val pool = source.filter { it.isMcq }
            .filter { request.subjectId == null || request.paper != null || it.subjectId == request.subjectId }
        if (pool.isEmpty()) throw DomainError.EmptyQuestionBank()

        val ordered = if (request.shuffle) pool.shuffled(random) else pool
        ordered.take(request.limit)
    }
}
