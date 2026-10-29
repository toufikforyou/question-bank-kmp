package dev.toufikforyou.questionbank.presentation.subject

import dev.toufikforyou.questionbank.domain.model.Board
import dev.toufikforyou.questionbank.domain.model.Catalog
import dev.toufikforyou.questionbank.domain.model.Chapter
import dev.toufikforyou.questionbank.domain.model.PaperKey
import dev.toufikforyou.questionbank.domain.model.PaperSummary
import dev.toufikforyou.questionbank.domain.model.QuizRequest
import dev.toufikforyou.questionbank.domain.model.Subject
import dev.toufikforyou.questionbank.domain.model.toBanglaDigits
import dev.toufikforyou.questionbank.domain.usecase.GetCatalogUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetPaperSummariesUseCase
import dev.toufikforyou.questionbank.presentation.format.Labels
import dev.toufikforyou.questionbank.presentation.mvi.BaseStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

data class PaperItem(
    val key: PaperKey,
    val boardName: String,
    val title: String,
    val countLabel: String,
)

data class YearSection(
    val year: Int,
    val title: String,
    val papers: List<PaperItem>,
    /** Why the year is empty or partial, e.g. the exam was not held. */
    val note: String?,
)

enum class SubjectTab { PAPERS, CHAPTERS }

data class SubjectState(
    val isLoading: Boolean = true,
    val subject: Subject? = null,
    val catalog: Catalog = Catalog.EMPTY,
    val papers: List<PaperSummary> = emptyList(),
    val tab: SubjectTab = SubjectTab.PAPERS,
    val boardFilter: String? = null,
    val errorMessage: String? = null,
) {
    val title: String get() = subject?.name.orEmpty()
    val subtitle: String
        get() = subject?.let { "${Labels.exam(it.exam)} · বিষয় কোড ${it.code.toBanglaDigits()} · ${Labels.group(it.group)}" }.orEmpty()
    val chapters: List<Chapter> get() = subject?.chapters.orEmpty()
    val boards: List<Board> get() = catalog.boards
    val paperCount: Int get() = papers.size
    val practiceRequest: QuizRequest? get() = subject?.let { QuizRequest.forSubject(it.id) }

    val yearSections: List<YearSection>
        get() {
            val subject = subject ?: return emptyList()
            val byYear = papers.filter { boardFilter == null || it.key.boardId == boardFilter }.groupBy { it.key.year }
            val noteYears = catalog.notes.filter { it.exam == subject.exam }.map { it.year }
            return (byYear.keys + noteYears).distinct().sortedDescending().map { year ->
                YearSection(
                    year = year,
                    title = "${Labels.number(year)} সাল",
                    papers = byYear[year].orEmpty().map { summary ->
                        val board = catalog.board(summary.key.boardId)?.name ?: summary.key.boardId
                        PaperItem(summary.key, board, Labels.boardYear(board, year), Labels.questionCount(summary.mcqCount, summary.cqCount))
                    },
                    note = catalog.noteFor(subject.exam, year)?.text,
                )
            }
        }

    fun chapterLabel(chapter: Chapter): String = Labels.chapter(chapter.number, chapter.name)

    fun chapterCountLabel(chapter: Chapter): String = Labels.questionCount(chapter.mcqCount, chapter.cqCount)
}

sealed class SubjectIntent {
    data class SelectTab(val tab: SubjectTab) : SubjectIntent()
    /** null shows every board. */
    data class FilterBoard(val boardId: String?) : SubjectIntent()
    data object Retry : SubjectIntent()
}

sealed class SubjectEffect

class SubjectStore(
    private val subjectId: String,
    private val getCatalog: GetCatalogUseCase,
    private val getPaperSummaries: GetPaperSummariesUseCase,
    dispatcher: CoroutineDispatcher,
) : BaseStore<SubjectState, SubjectIntent, SubjectEffect>(SubjectState(), dispatcher) {

    init {
        load()
    }

    override fun dispatch(intent: SubjectIntent) {
        when (intent) {
            is SubjectIntent.SelectTab -> setState { copy(tab = intent.tab) }
            is SubjectIntent.FilterBoard -> setState { copy(boardFilter = intent.boardId) }
            SubjectIntent.Retry -> load()
        }
    }

    private fun load() {
        setState { copy(isLoading = true, errorMessage = null) }
        scope.launch {
            val catalog = getCatalog().getOrElse { return@launch fail(it) }
            val papers = getPaperSummaries(subjectId).getOrElse { return@launch fail(it) }
            setState { copy(isLoading = false, catalog = catalog, subject = catalog.subject(subjectId), papers = papers) }
        }
    }

    private fun fail(error: Throwable) {
        setState { copy(isLoading = false, errorMessage = error.message ?: "বিষয়টি লোড করা যায়নি।") }
    }
}
