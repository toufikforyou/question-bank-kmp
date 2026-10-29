package dev.toufikforyou.questionbank.domain

import dev.toufikforyou.questionbank.domain.model.DomainError
import dev.toufikforyou.questionbank.domain.model.ExamType
import dev.toufikforyou.questionbank.domain.model.PaperKey
import dev.toufikforyou.questionbank.domain.usecase.GetChapterQuestionsUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetPaperSummariesUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetPaperUseCase
import dev.toufikforyou.questionbank.domain.usecase.SearchQuestionsUseCase
import dev.toufikforyou.questionbank.fakes.FakeQuestionRepository
import dev.toufikforyou.questionbank.fakes.HSC_ICT
import dev.toufikforyou.questionbank.fakes.PHYSICS
import dev.toufikforyou.questionbank.fakes.PHYSICS_CH1
import dev.toufikforyou.questionbank.fakes.at
import dev.toufikforyou.questionbank.fakes.creative
import dev.toufikforyou.questionbank.fakes.mcq
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CatalogUseCasesTest {

    private val repo = FakeQuestionRepository(
        listOf(
            mcq("m1", appearances = listOf(at("dhaka", 2023, 1), at("rajshahi", 2024, 2)), prompt = "বলের একক কী?"),
            mcq("m2", appearances = listOf(at("dhaka", 2023, 2)), prompt = "বেগের মাত্রা"),
            creative("c1", appearances = listOf(at("dhaka", 2023, 3))),
            mcq("ict", subjectId = HSC_ICT, chapterId = "hsc-ict-1", appearances = listOf(at("dhaka", 2021, 1)), prompt = "বলের ধারণা ICT"),
        ),
    )

    @Test
    fun paperReturnsQuestionsInPaperOrderWithBoardAndSubject() = runTest {
        val paper = GetPaperUseCase(repo)(PaperKey(PHYSICS, "dhaka", 2023)).getOrThrow()

        assertEquals(listOf("m1", "m2", "c1"), paper.questions.map { it.id })
        assertEquals("ঢাকা", paper.board.name)
        assertEquals(2, paper.mcqs.size)
        assertEquals(1, paper.creativeQuestions.size)
    }

    @Test
    fun missingPaperExplainsYearNote() = runTest {
        val error = GetPaperUseCase(repo)(PaperKey(HSC_ICT, "dhaka", 2020)).exceptionOrNull()

        assertIs<DomainError.PaperNotFound>(error)
        assertEquals("২০২০ সালের পরীক্ষা অনুষ্ঠিত হয়নি।", error.message)
    }

    @Test
    fun missingPaperWithoutNoteUsesGenericMessage() = runTest {
        val error = GetPaperUseCase(repo)(PaperKey(PHYSICS, "rajshahi", 2015)).exceptionOrNull()

        assertIs<DomainError.PaperNotFound>(error)
        assertEquals(null, error.note)
    }

    @Test
    fun unknownSubjectIsRejected() = runTest {
        assertIs<DomainError.SubjectNotFound>(GetPaperUseCase(repo)(PaperKey("nope", "dhaka", 2023)).exceptionOrNull())
        assertIs<DomainError.SubjectNotFound>(GetPaperSummariesUseCase(repo)("nope").exceptionOrNull())
    }

    @Test
    fun summariesAreNewestFirstThenBoardOrder() = runTest {
        val summaries = GetPaperSummariesUseCase(repo)(PHYSICS).getOrThrow()

        assertEquals(listOf(PaperKey(PHYSICS, "rajshahi", 2024), PaperKey(PHYSICS, "dhaka", 2023)), summaries.map { it.key })
        assertEquals(2, summaries.last().mcqCount)
        assertEquals(1, summaries.last().cqCount)
    }

    @Test
    fun chapterQuestionsPutMostRepeatedFirst() = runTest {
        val questions = GetChapterQuestionsUseCase(repo)(PHYSICS_CH1).getOrThrow()

        assertEquals("m1", questions.first().id)
        assertEquals(2, questions.first().repeatCount)
        assertIs<DomainError.ChapterNotFound>(GetChapterQuestionsUseCase(repo)("nope").exceptionOrNull())
    }

    @Test
    fun searchNormalizesQueryAndStaysWithinExam() = runTest {
        val results = SearchQuestionsUseCase(repo)("  বলের   ", ExamType.SSC).getOrThrow()

        assertEquals("বলের", repo.lastSearch)
        assertEquals(listOf("m1"), results.map { it.id })
    }

    @Test
    fun searchRejectsTooShortQueries() = runTest {
        assertIs<DomainError.QueryTooShort>(SearchQuestionsUseCase(repo)(" ক ", ExamType.SSC).exceptionOrNull())
    }
}
