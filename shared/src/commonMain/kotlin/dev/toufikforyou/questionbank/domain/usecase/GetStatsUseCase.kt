package dev.toufikforyou.questionbank.domain.usecase

import dev.toufikforyou.questionbank.domain.model.AnswerRecord
import dev.toufikforyou.questionbank.domain.model.Category
import dev.toufikforyou.questionbank.domain.model.CategoryStats
import dev.toufikforyou.questionbank.domain.model.UserStats
import dev.toufikforyou.questionbank.domain.model.percentOf
import dev.toufikforyou.questionbank.domain.repository.AttemptRepository
import dev.toufikforyou.questionbank.domain.repository.QuestionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class GetStatsUseCase(
    private val attemptRepository: AttemptRepository,
    private val questionRepository: QuestionRepository,
) {
    operator fun invoke(): Flow<UserStats> = combine(
        attemptRepository.observeAttempts(),
        questionRepository.observeCategories(),
        questionRepository.observeBookmarkedQuestions(),
    ) { attempts, categories, bookmarks ->
        StatsCalculator.calculate(attempts, categories, bookmarks.size)
    }.distinctUntilChanged()
}

/** Pure scoring logic, kept separate so it can be unit tested exhaustively. */
object StatsCalculator {
    fun calculate(
        attempts: List<AnswerRecord>,
        categories: List<Category>,
        bookmarkedCount: Int,
    ): UserStats {
        val ordered = attempts.sortedBy { it.answeredAtMillis }
        val totalCorrect = ordered.count { it.isCorrect }

        var run = 0
        var best = 0
        for (attempt in ordered) {
            run = if (attempt.isCorrect) run + 1 else 0
            if (run > best) best = run
        }

        val byCategory = ordered.groupBy { it.categoryId }
        val categoryStats = categories.map { category ->
            val answers = byCategory[category.id].orEmpty()
            val correct = answers.count { it.isCorrect }
            CategoryStats(
                categoryId = category.id,
                title = category.title,
                answered = answers.size,
                correct = correct,
                accuracyPercent = percentOf(correct, answers.size),
            )
        }

        return UserStats(
            totalAnswered = ordered.size,
            totalCorrect = totalCorrect,
            accuracyPercent = percentOf(totalCorrect, ordered.size),
            currentStreak = run,
            bestStreak = best,
            bookmarkedCount = bookmarkedCount,
            categoryStats = categoryStats,
        )
    }
}
