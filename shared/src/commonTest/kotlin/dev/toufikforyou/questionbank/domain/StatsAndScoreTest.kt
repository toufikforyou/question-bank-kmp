package dev.toufikforyou.questionbank.domain

import app.cash.turbine.test
import dev.toufikforyou.questionbank.domain.model.AnswerResult
import dev.toufikforyou.questionbank.domain.model.QuizScore
import dev.toufikforyou.questionbank.domain.model.QuizSession
import dev.toufikforyou.questionbank.domain.usecase.GetStatsUseCase
import dev.toufikforyou.questionbank.domain.usecase.StatsCalculator
import dev.toufikforyou.questionbank.fakes.FakeAttemptRepository
import dev.toufikforyou.questionbank.fakes.FakeQuestionRepository
import dev.toufikforyou.questionbank.fakes.HSC_ICT
import dev.toufikforyou.questionbank.fakes.PHYSICS
import dev.toufikforyou.questionbank.fakes.attempt
import dev.toufikforyou.questionbank.fakes.mcq
import dev.toufikforyou.questionbank.fakes.testCatalog
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StatsAndScoreTest {

    private val subjects = testCatalog().subjects

    @Test
    fun noAttemptsProducesZeroedStatsWithoutDividingByZero() {
        val stats = StatsCalculator.calculate(emptyList(), subjects, bookmarkedCount = 0)

        assertEquals(0, stats.totalAnswered)
        assertEquals(0, stats.accuracyPercent)
        assertTrue(stats.subjectStats.isEmpty(), "only practised subjects are listed")
    }

    @Test
    fun accuracyIsRoundedAndStreaksAreTrackedPerSubject() {
        val attempts = listOf(
            attempt(HSC_ICT, isCorrect = true, at = 6),
            attempt(PHYSICS, isCorrect = true, at = 1),
            attempt(PHYSICS, isCorrect = true, at = 2),
            attempt(HSC_ICT, isCorrect = true, at = 3),
            attempt(PHYSICS, isCorrect = false, at = 4),
            attempt(HSC_ICT, isCorrect = true, at = 5),
        )

        val stats = StatsCalculator.calculate(attempts, subjects, bookmarkedCount = 3)

        assertEquals(6, stats.totalAnswered)
        assertEquals(83, stats.accuracyPercent)
        assertEquals(3, stats.bestStreak)
        assertEquals(2, stats.currentStreak)
        assertEquals(67, stats.subjectStats.first { it.subjectId == PHYSICS }.accuracyPercent)
        assertEquals(100, stats.subjectStats.first { it.subjectId == HSC_ICT }.accuracyPercent)
    }

    @Test
    fun getStatsEmitsUpdatedStatsWhenAttemptsChange() = runTest {
        val attempts = FakeAttemptRepository()
        val useCase = GetStatsUseCase(attempts, FakeQuestionRepository(listOf(mcq("q1"))))

        useCase().test {
            assertEquals(0, awaitItem().totalAnswered)
            attempts.record(attempt(PHYSICS, isCorrect = true, at = 1))
            val updated = awaitItem()
            assertEquals(1, updated.totalAnswered)
            assertEquals("পদার্থবিজ্ঞান", updated.subjectStats.single().title)
        }
    }

    @Test
    fun quizScoreBoundaries() {
        assertEquals(0, QuizScore(0, 0).percent)
        assertFalse(QuizScore(0, 0).isPassing)
        assertTrue(QuizScore(3, 5).isPassing)
        assertFailsWith<IllegalArgumentException> { QuizScore(correct = 6, total = 5) }
    }

    @Test
    fun sessionScoreCountsUnansweredQuestionsAsWrong() {
        val session = QuizSession(listOf(mcq("q1"), mcq("q2"), mcq("q3")))
            .record(AnswerResult("q1", "ক", "ক", isCorrect = true, explanation = null))

        assertEquals(QuizScore(correct = 1, total = 3), session.score)
        assertEquals(33, session.score.percent)
    }
}
