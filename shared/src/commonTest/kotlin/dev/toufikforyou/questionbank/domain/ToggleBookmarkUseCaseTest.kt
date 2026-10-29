package dev.toufikforyou.questionbank.domain

import app.cash.turbine.test
import dev.toufikforyou.questionbank.domain.model.DomainError
import dev.toufikforyou.questionbank.domain.usecase.ToggleBookmarkUseCase
import dev.toufikforyou.questionbank.fakes.FakeQuestionRepository
import dev.toufikforyou.questionbank.fakes.question
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ToggleBookmarkUseCaseTest {

    @Test
    fun togglingTwiceRestoresOriginalState() = runTest {
        val repo = FakeQuestionRepository(listOf(question("q1")))
        val toggle = ToggleBookmarkUseCase(repo)

        repo.observeBookmarkedQuestions().test {
            assertEquals(emptyList(), awaitItem())

            assertTrue(toggle("q1").getOrThrow())
            assertEquals(listOf("q1"), awaitItem().map { it.id })

            assertFalse(toggle("q1").getOrThrow())
            assertEquals(emptyList(), awaitItem())
        }
    }

    @Test
    fun unknownQuestionFails() = runTest {
        val result = ToggleBookmarkUseCase(FakeQuestionRepository())("missing")

        assertIs<DomainError.QuestionNotFound>(result.exceptionOrNull())
    }
}
