package dev.toufikforyou.questionbank.domain

import dev.toufikforyou.questionbank.domain.model.DomainError
import dev.toufikforyou.questionbank.domain.model.QuizRequest
import dev.toufikforyou.questionbank.domain.usecase.GetQuestionsUseCase
import dev.toufikforyou.questionbank.fakes.FakeQuestionRepository
import dev.toufikforyou.questionbank.fakes.question
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GetQuestionsUseCaseTest {

    private val questions = (1..20).map { question("q$it", categoryId = if (it % 2 == 0) "even" else "odd") }

    @Test
    fun emptyQuestionBankFailsWithDomainError() = runTest {
        val useCase = GetQuestionsUseCase(FakeQuestionRepository(emptyList()))

        val error = useCase(QuizRequest()).exceptionOrNull()

        assertIs<DomainError.EmptyQuestionBank>(error)
    }

    @Test
    fun unknownCategoryIsTreatedAsEmpty() = runTest {
        val useCase = GetQuestionsUseCase(FakeQuestionRepository(questions))

        val error = useCase(QuizRequest(categoryId = "missing")).exceptionOrNull()

        assertIs<DomainError.EmptyQuestionBank>(error)
        assertEquals("missing", error.categoryId)
    }

    @Test
    fun limitBelowOneIsRejected() = runTest {
        val result = GetQuestionsUseCase(FakeQuestionRepository(questions))(QuizRequest(limit = 0))

        assertIs<DomainError.InvalidQuizSize>(result.exceptionOrNull())
    }

    @Test
    fun limitAboveMaximumIsRejected() = runTest {
        val result = GetQuestionsUseCase(FakeQuestionRepository(questions))(QuizRequest(limit = QuizRequest.MAX_LIMIT + 1))

        assertIs<DomainError.InvalidQuizSize>(result.exceptionOrNull())
    }

    @Test
    fun limitIsAppliedAndNeverExceedsPoolSize() = runTest {
        val useCase = GetQuestionsUseCase(FakeQuestionRepository(questions))

        assertEquals(5, useCase(QuizRequest(limit = 5)).getOrThrow().size)
        assertEquals(20, useCase(QuizRequest(limit = QuizRequest.MAX_LIMIT)).getOrThrow().size)
    }

    @Test
    fun categoryFilterOnlyReturnsMatchingQuestions() = runTest {
        val result = GetQuestionsUseCase(FakeQuestionRepository(questions))(QuizRequest(categoryId = "even", limit = 50))

        val selected = result.getOrThrow()
        assertEquals(10, selected.size)
        assertTrue(selected.all { it.categoryId == "even" })
    }

    @Test
    fun shuffleIsDeterministicForSeededRandom() = runTest {
        val repo = FakeQuestionRepository(questions)

        val first = GetQuestionsUseCase(repo, Random(42))(QuizRequest()).getOrThrow().map { it.id }
        val second = GetQuestionsUseCase(repo, Random(42))(QuizRequest()).getOrThrow().map { it.id }

        assertEquals(first, second)
        assertEquals(first.size, first.toSet().size, "a quiz must never repeat a question")
    }

    @Test
    fun bookmarkedOnlyUsesBookmarks() = runTest {
        val repo = FakeQuestionRepository(listOf(question("a", bookmarked = true), question("b")))

        val result = GetQuestionsUseCase(repo)(QuizRequest(bookmarkedOnly = true)).getOrThrow()

        assertEquals(listOf("a"), result.map { it.id })
    }

    @Test
    fun repositoryFailureIsWrappedInResult() = runTest {
        val repo = FakeQuestionRepository(questions).apply { failWith = IllegalStateException("disk full") }

        val result = GetQuestionsUseCase(repo)(QuizRequest())

        assertEquals("disk full", result.exceptionOrNull()?.message)
    }
}
