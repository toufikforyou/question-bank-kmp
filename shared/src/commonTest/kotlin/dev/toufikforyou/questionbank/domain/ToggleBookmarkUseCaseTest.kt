package dev.toufikforyou.questionbank.domain

import app.cash.turbine.test
import dev.toufikforyou.questionbank.domain.model.DomainError
import dev.toufikforyou.questionbank.domain.usecase.ToggleBookmarkUseCase
import dev.toufikforyou.questionbank.fakes.FakeQuestionRepository
import dev.toufikforyou.questionbank.fakes.creative
import dev.toufikforyou.questionbank.fakes.mcq
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ToggleBookmarkUseCaseTest {

    @Test
    fun togglingTwiceRestoresOriginalStateForMcqAndCreative() = runTest {
        val repo = FakeQuestionRepository(listOf(mcq("q1"), creative("cq1")))
        val toggle = ToggleBookmarkUseCase(repo)

        repo.observeBookmarkedQuestions().test {
            assertEquals(emptyList(), awaitItem())

            assertTrue(toggle("q1").getOrThrow())
            assertEquals(listOf("q1"), awaitItem().map { it.id })

            assertTrue(toggle("cq1").getOrThrow())
            assertEquals(listOf("cq1", "q1"), awaitItem().map { it.id })

            assertFalse(toggle("q1").getOrThrow())
            assertEquals(listOf("cq1"), awaitItem().map { it.id })
        }
    }

    @Test
    fun unknownQuestionFails() = runTest {
        assertIs<DomainError.QuestionNotFound>(ToggleBookmarkUseCase(FakeQuestionRepository())("missing").exceptionOrNull())
    }
}
