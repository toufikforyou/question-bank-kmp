package dev.toufikforyou.questionbank.domain

import dev.toufikforyou.questionbank.domain.model.DomainError
import dev.toufikforyou.questionbank.domain.usecase.AnswerQuestionUseCase
import dev.toufikforyou.questionbank.fakes.FakeAttemptRepository
import dev.toufikforyou.questionbank.fakes.FakeQuestionRepository
import dev.toufikforyou.questionbank.fakes.FixedTimeProvider
import dev.toufikforyou.questionbank.fakes.question
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AnswerQuestionUseCaseTest {

    private val attempts = FakeAttemptRepository()
    private val useCase = AnswerQuestionUseCase(
        questionRepository = FakeQuestionRepository(listOf(question("q1", categoryId = "math", correctOptionId = "b"))),
        attemptRepository = attempts,
        timeProvider = FixedTimeProvider(now = 500L),
    )

    @Test
    fun correctAnswerIsScoredAndRecorded() = runTest {
        val result = useCase("q1", "b").getOrThrow()

        assertTrue(result.isCorrect)
        assertEquals("b", result.correctOptionId)
        val recorded = attempts.recorded.single()
        assertEquals("math", recorded.categoryId)
        assertTrue(recorded.isCorrect)
        assertEquals(500L, recorded.answeredAtMillis)
    }

    @Test
    fun wrongAnswerRevealsCorrectOption() = runTest {
        val result = useCase("q1", "a").getOrThrow()

        assertFalse(result.isCorrect)
        assertEquals("a", result.selectedOptionId)
        assertEquals("b", result.correctOptionId)
        assertFalse(attempts.recorded.single().isCorrect)
    }

    @Test
    fun optionFromAnotherQuestionIsRejectedAndNotRecorded() = runTest {
        val result = useCase("q1", "z")

        assertIs<DomainError.InvalidOption>(result.exceptionOrNull())
        assertTrue(attempts.recorded.isEmpty())
    }

    @Test
    fun unknownQuestionIsRejected() = runTest {
        assertIs<DomainError.QuestionNotFound>(useCase("nope", "a").exceptionOrNull())
    }
}
