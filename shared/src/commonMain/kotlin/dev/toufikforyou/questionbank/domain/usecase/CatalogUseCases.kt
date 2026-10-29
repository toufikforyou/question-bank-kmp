package dev.toufikforyou.questionbank.domain.usecase

import dev.toufikforyou.questionbank.domain.model.Catalog
import dev.toufikforyou.questionbank.domain.model.DomainError
import dev.toufikforyou.questionbank.domain.model.PaperKey
import dev.toufikforyou.questionbank.domain.model.PaperSummary
import dev.toufikforyou.questionbank.domain.model.Question
import dev.toufikforyou.questionbank.domain.model.QuestionPaper
import dev.toufikforyou.questionbank.domain.repository.QuestionRepository

class GetCatalogUseCase(
    private val repository: QuestionRepository,
) {
    suspend operator fun invoke(): Result<Catalog> = resultOf { repository.getCatalog() }
}

/** Papers on record for a subject, newest year first, boards in official order. */
class GetPaperSummariesUseCase(
    private val repository: QuestionRepository,
) {
    suspend operator fun invoke(subjectId: String): Result<List<PaperSummary>> = resultOf {
        val catalog = repository.getCatalog()
        catalog.subject(subjectId) ?: throw DomainError.SubjectNotFound(subjectId)
        val boardOrder = catalog.boards.mapIndexed { index, board -> board.id to index }.toMap()
        repository.getPaperSummaries(subjectId)
            .sortedWith(compareByDescending<PaperSummary> { it.key.year }.thenBy { boardOrder[it.key.boardId] ?: Int.MAX_VALUE })
    }
}

/** Opens one board paper. Missing papers explain themselves (e.g. "exam not held that year"). */
class GetPaperUseCase(
    private val repository: QuestionRepository,
) {
    suspend operator fun invoke(key: PaperKey): Result<QuestionPaper> = resultOf {
        val catalog = repository.getCatalog()
        val subject = catalog.subject(key.subjectId) ?: throw DomainError.SubjectNotFound(key.subjectId)
        val board = catalog.board(key.boardId) ?: throw DomainError.PaperNotFound(key, note = null)
        val questions = repository.getPaperQuestions(key)
        if (questions.isEmpty()) {
            throw DomainError.PaperNotFound(key, catalog.noteFor(subject.exam, key.year)?.text)
        }
        QuestionPaper(key, subject, board, questions)
    }
}

/** All board questions from one chapter across every board and year; most repeated first. */
class GetChapterQuestionsUseCase(
    private val repository: QuestionRepository,
) {
    suspend operator fun invoke(chapterId: String): Result<List<Question>> = resultOf {
        repository.getCatalog().chapter(chapterId) ?: throw DomainError.ChapterNotFound(chapterId)
        repository.getChapterQuestions(chapterId)
    }
}
