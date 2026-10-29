package dev.toufikforyou.questionbank.domain

import dev.toufikforyou.questionbank.domain.model.DomainError
import dev.toufikforyou.questionbank.domain.usecase.AnswerQuestionUseCase
import dev.toufikforyou.questionbank.fakes.FakeAttemptRepository
import dev.toufikforyou.questionbank.fakes.FakeQuestionRepository
import dev.toufikforyou.questionbank.fakes.FixedTimeProvider
import dev.toufikforyou.questionbank.fakes.PHYSICS
import dev.toufikforyou.questionbank.fakes.creative
import dev.toufikforyou.questionbank.fakes.mcq
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AnswerQuestionUseCaseTest {

    private val attempts = FakeAttemptRepository()
    private val useCase = AnswerQuestionUseCase(
        questionRepository = FakeQuestionRepository(listOf(mcq("q1", correctOptionId = "খ"), creative("cq1"))),
        attemptRepository = attempts,
        timeProvider = FixedTimeProvider(now = 500L),
    )

    @Test
    fun correctAnswerIsScoredAndRecordedAgainstItsSubject() = runTest {
        val result = useCase("q1", "খ").getOrThrow()

        assertTrue(result.isCorrect)
        val recorded = attempts.recorded.single()
        assertEquals(PHYSICS, recorded.subjectId)
        assertEquals(500L, recorded.answeredAtMillis)
    }

    @Test
    fun wrongAnswerRevealsCorrectOption() = runTest {
        val result = useCase("q1", "ক").getOrThrow()

        assertFalse(result.isCorrect)
        assertEquals("খ", result.correctOptionId)
        assertEquals("ব্যাখ্যা q1", result.explanation)
    }

    @Test
    fun foreignOptionIsRejectedAndNotRecorded() = runTest {
        assertIs<DomainError.InvalidOption>(useCase("q1", "z").exceptionOrNull())
        assertTrue(attempts.recorded.isEmpty())
    }

    @Test
    fun creativeQuestionsCannotBeAnsweredAsMcq() = runTest {
        assertIs<DomainError.InvalidOption>(useCase("cq1", "ক").exceptionOrNull())
    }

    @Test
    fun unknownQuestionIsRejected() = runTest {
        assertIs<DomainError.QuestionNotFound>(useCase("nope", "ক").exceptionOrNull())
    }
}
