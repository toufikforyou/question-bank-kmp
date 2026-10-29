package dev.toufikforyou.questionbank.domain

import app.cash.turbine.test
import dev.toufikforyou.questionbank.domain.model.AnswerResult
import dev.toufikforyou.questionbank.domain.model.Category
import dev.toufikforyou.questionbank.domain.model.QuizScore
import dev.toufikforyou.questionbank.domain.model.QuizSession
import dev.toufikforyou.questionbank.domain.model.UserStats
import dev.toufikforyou.questionbank.domain.usecase.GetStatsUseCase
import dev.toufikforyou.questionbank.domain.usecase.StatsCalculator
import dev.toufikforyou.questionbank.fakes.FakeAttemptRepository
import dev.toufikforyou.questionbank.fakes.FakeQuestionRepository
import dev.toufikforyou.questionbank.fakes.attempt
import dev.toufikforyou.questionbank.fakes.question
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StatsAndScoreTest {

    private val categories = listOf(Category("math", "Math", "", 2), Category("geo", "Geo", "", 2))

    @Test
    fun noAttemptsProducesZeroedStatsWithoutDividingByZero() {
        val stats = StatsCalculator.calculate(emptyList(), categories, bookmarkedCount = 0)

        assertEquals(0, stats.totalAnswered)
        assertEquals(0, stats.accuracyPercent)
        assertEquals(listOf(0, 0), stats.categoryStats.map { it.accuracyPercent })
    }

    @Test
    fun accuracyIsRoundedAndStreaksAreTracked() {
        // correct, correct, correct, wrong, correct, correct (out of chronological order on purpose)
        val attempts = listOf(
            attempt("geo", isCorrect = true, at = 6),
            attempt("math", isCorrect = true, at = 1),
            attempt("math", isCorrect = true, at = 2),
            attempt("geo", isCorrect = true, at = 3),
            attempt("math", isCorrect = false, at = 4),
            attempt("geo", isCorrect = true, at = 5),
        )

        val stats = StatsCalculator.calculate(attempts, categories, bookmarkedCount = 3)

        assertEquals(6, stats.totalAnswered)
        assertEquals(5, stats.totalCorrect)
        assertEquals(83, stats.accuracyPercent) // 5 / 6 = 83.33 -> 83
        assertEquals(3, stats.bestStreak)
        assertEquals(2, stats.currentStreak)
        assertEquals(3, stats.bookmarkedCount)
        assertEquals(67, stats.categoryStats.first { it.categoryId == "math" }.accuracyPercent)
        assertEquals(100, stats.categoryStats.first { it.categoryId == "geo" }.accuracyPercent)
    }

    @Test
    fun wrongLastAnswerResetsCurrentStreak() {
        val stats = StatsCalculator.calculate(
            listOf(attempt("math", true, 1), attempt("math", false, 2)),
            categories,
            bookmarkedCount = 0,
        )

        assertEquals(0, stats.currentStreak)
        assertEquals(1, stats.bestStreak)
    }

    @Test
    fun getStatsEmitsUpdatedStatsWhenAttemptsChange() = runTest {
        val attempts = FakeAttemptRepository()
        val useCase = GetStatsUseCase(attempts, FakeQuestionRepository(listOf(question("q1")), categories))

        useCase().test {
            assertEquals(0, awaitItem().totalAnswered)
            attempts.record(attempt("math", isCorrect = true, at = 1))
            val updated: UserStats = awaitItem()
            assertEquals(1, updated.totalAnswered)
            assertEquals(100, updated.accuracyPercent)
        }
    }

    @Test
    fun quizScoreBoundaries() {
        assertEquals(0, QuizScore(0, 0).percent)
        assertFalse(QuizScore(0, 0).isPassing)
        assertEquals(60, QuizScore(3, 5).percent)
        assertTrue(QuizScore(3, 5).isPassing)
        assertFailsWith<IllegalArgumentException> { QuizScore(correct = 6, total = 5) }
        assertFailsWith<IllegalArgumentException> { QuizScore(correct = -1, total = 5) }
    }

    @Test
    fun sessionScoreCountsOnlyCorrectAnswersAndReanswerOverwrites() {
        val questions = listOf(question("q1"), question("q2"), question("q3"))
        val session = QuizSession(questions)
            .record(AnswerResult("q1", "a", "a", isCorrect = true, explanation = null))
            .record(AnswerResult("q2", "b", "a", isCorrect = false, explanation = null))
            .record(AnswerResult("q2", "a", "a", isCorrect = true, explanation = null))

        assertEquals(QuizScore(correct = 2, total = 3), session.score)
        assertEquals(67, session.score.percent)
    }

    @Test
    fun sessionAdvanceStopsAtEnd() {
        val session = QuizSession(listOf(question("q1"))).advance()

        assertTrue(session.isFinished)
        assertEquals(session, session.advance())
        assertFailsWith<IllegalArgumentException> {
            QuizSession(listOf(question("q1"))).record(AnswerResult("other", "a", "a", true, null))
        }
    }
}
