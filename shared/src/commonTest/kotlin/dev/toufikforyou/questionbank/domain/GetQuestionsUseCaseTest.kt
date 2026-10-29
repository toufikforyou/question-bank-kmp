package dev.toufikforyou.questionbank.domain

import dev.toufikforyou.questionbank.domain.model.DomainError
import dev.toufikforyou.questionbank.domain.model.PaperKey
import dev.toufikforyou.questionbank.domain.model.QuizRequest
import dev.toufikforyou.questionbank.domain.usecase.GetQuestionsUseCase
import dev.toufikforyou.questionbank.fakes.FakeQuestionRepository
import dev.toufikforyou.questionbank.fakes.PHYSICS
import dev.toufikforyou.questionbank.fakes.PHYSICS_CH2
import dev.toufikforyou.questionbank.fakes.at
import dev.toufikforyou.questionbank.fakes.creative
import dev.toufikforyou.questionbank.fakes.mcq
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GetQuestionsUseCaseTest {

    private val questions = (1..20).map { mcq("q$it", chapterId = if (it % 2 == 0) PHYSICS_CH2 else "ssc-physics-1") } +
        creative("cq1")

    @Test
    fun emptyQuestionBankFailsWithDomainError() = runTest {
        val result = GetQuestionsUseCase(FakeQuestionRepository())(QuizRequest.forSubject(PHYSICS))

        assertIs<DomainError.EmptyQuestionBank>(result.exceptionOrNull())
    }

    @Test
    fun requestWithoutAnySourceIsRejected() = runTest {
        val result = GetQuestionsUseCase(FakeQuestionRepository(questions))(QuizRequest())

        assertIs<DomainError.EmptyQuestionBank>(result.exceptionOrNull())
    }

    @Test
    fun limitOutsideBoundsIsRejected() = runTest {
        val useCase = GetQuestionsUseCase(FakeQuestionRepository(questions))

        assertIs<DomainError.InvalidQuizSize>(useCase(QuizRequest(subjectId = PHYSICS, limit = 0)).exceptionOrNull())
        assertIs<DomainError.InvalidQuizSize>(useCase(QuizRequest(subjectId = PHYSICS, limit = QuizRequest.MAX_LIMIT + 1)).exceptionOrNull())
    }

    @Test
    fun creativeQuestionsAreNeverPractisedAsMcq() = runTest {
        val selected = GetQuestionsUseCase(FakeQuestionRepository(questions))(QuizRequest(subjectId = PHYSICS, limit = 50)).getOrThrow()

        assertEquals(20, selected.size)
        assertTrue(selected.all { it.isMcq })
    }

    @Test
    fun chapterRequestOnlyUsesThatChapter() = runTest {
        val selected = GetQuestionsUseCase(FakeQuestionRepository(questions))(QuizRequest(chapterId = PHYSICS_CH2, limit = 50)).getOrThrow()

        assertEquals(10, selected.size)
        assertTrue(selected.all { it.chapterId == PHYSICS_CH2 })
    }

    @Test
    fun paperRequestKeepsOriginalPaperOrderAndIsTimed() = runTest {
        val repo = FakeQuestionRepository(
            listOf(
                mcq("third", appearances = listOf(at("dhaka", 2023, 3))),
                mcq("first", appearances = listOf(at("dhaka", 2023, 1))),
                creative("cq", appearances = listOf(at("dhaka", 2023, 2))),
                mcq("other-board", appearances = listOf(at("rajshahi", 2023, 1))),
            ),
        )
        val request = QuizRequest.forPaper(PaperKey(PHYSICS, "dhaka", 2023))

        val selected = GetQuestionsUseCase(repo, Random(1))(request).getOrThrow()

        assertEquals(listOf("first", "third"), selected.map { it.id })
        assertTrue(request.timed)
    }

    @Test
    fun shuffleIsDeterministicForSeededRandomAndNeverRepeats() = runTest {
        val repo = FakeQuestionRepository(questions)

        val first = GetQuestionsUseCase(repo, Random(42))(QuizRequest.forSubject(PHYSICS)).getOrThrow().map { it.id }
        val second = GetQuestionsUseCase(repo, Random(42))(QuizRequest.forSubject(PHYSICS)).getOrThrow().map { it.id }

        assertEquals(first, second)
        assertEquals(QuizRequest.DEFAULT_LIMIT, first.size)
        assertEquals(first.size, first.toSet().size)
    }

    @Test
    fun bookmarksRequestUsesBookmarkedMcqsOnly() = runTest {
        val repo = FakeQuestionRepository(listOf(mcq("a", bookmarked = true), mcq("b")))

        val selected = GetQuestionsUseCase(repo)(QuizRequest.forBookmarks()).getOrThrow()

        assertEquals(listOf("a"), selected.map { it.id })
    }

    @Test
    fun repositoryFailureIsWrappedInResult() = runTest {
        val repo = FakeQuestionRepository(questions).apply { failWith = IllegalStateException("disk full") }

        val result = GetQuestionsUseCase(repo)(QuizRequest.forPaper(PaperKey(PHYSICS, "dhaka", 2023)))

        assertEquals("disk full", result.exceptionOrNull()?.message)
    }
}
