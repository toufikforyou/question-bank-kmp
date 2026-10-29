package dev.toufikforyou.questionbank.domain.usecase

import dev.toufikforyou.questionbank.domain.model.AnswerRecord
import dev.toufikforyou.questionbank.domain.model.Subject
import dev.toufikforyou.questionbank.domain.model.SubjectStats
import dev.toufikforyou.questionbank.domain.model.UserStats
import dev.toufikforyou.questionbank.domain.model.percentOf
import dev.toufikforyou.questionbank.domain.repository.AttemptRepository
import dev.toufikforyou.questionbank.domain.repository.QuestionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

class GetStatsUseCase(
    private val attemptRepository: AttemptRepository,
    private val questionRepository: QuestionRepository,
) {
    operator fun invoke(): Flow<UserStats> = combine(
        attemptRepository.observeAttempts(),
        flow { emit(questionRepository.getCatalog().subjects) },
        questionRepository.observeBookmarkedQuestions(),
    ) { attempts, subjects, bookmarks ->
        StatsCalculator.calculate(attempts, subjects, bookmarks.size)
    }.distinctUntilChanged()
}

/** Pure scoring logic, kept separate so it can be unit tested exhaustively. */
object StatsCalculator {
    fun calculate(
        attempts: List<AnswerRecord>,
        subjects: List<Subject>,
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

        val bySubject = ordered.groupBy { it.subjectId }
        // Only subjects the learner has practised, so the list stays short and relevant.
        val subjectStats = subjects.filter { it.id in bySubject }.map { subject ->
            val answers = bySubject[subject.id].orEmpty()
            val correct = answers.count { it.isCorrect }
            SubjectStats(
                subjectId = subject.id,
                exam = subject.exam,
                title = subject.name,
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
            subjectStats = subjectStats,
        )
    }
}
