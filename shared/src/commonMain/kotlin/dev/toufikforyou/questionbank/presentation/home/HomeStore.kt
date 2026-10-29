package dev.toufikforyou.questionbank.presentation.home

import dev.toufikforyou.questionbank.domain.model.Board
import dev.toufikforyou.questionbank.domain.model.Catalog
import dev.toufikforyou.questionbank.domain.model.ExamType
import dev.toufikforyou.questionbank.domain.model.PaperKey
import dev.toufikforyou.questionbank.domain.model.StudyGroup
import dev.toufikforyou.questionbank.domain.model.Subject
import dev.toufikforyou.questionbank.domain.model.UserStats
import dev.toufikforyou.questionbank.domain.usecase.GetCatalogUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetStatsUseCase
import dev.toufikforyou.questionbank.domain.usecase.InitializeAppUseCase
import dev.toufikforyou.questionbank.presentation.format.Labels
import dev.toufikforyou.questionbank.presentation.mvi.BaseStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class SubjectSection(
    val title: String,
    val subjects: List<Subject>,
)

data class HomeState(
    val isLoading: Boolean = true,
    val profileName: String = "",
    val catalog: Catalog = Catalog.EMPTY,
    val selectedExam: ExamType = ExamType.SSC,
    val stats: UserStats = UserStats.EMPTY,
    val finderSubjectId: String? = null,
    val finderBoardId: String? = null,
    val finderYear: Int? = null,
    val errorMessage: String? = null,
) {
    val hasContent: Boolean get() = catalog.subjects.isNotEmpty()
    val isSample: Boolean get() = catalog.isSample
    val exams: List<ExamType> get() = catalog.exams.map { it.type }
    val boards: List<Board> get() = catalog.boards
    val years: List<Int> get() = catalog.yearsFor(selectedExam)
    val subjects: List<Subject> get() = catalog.subjectsFor(selectedExam)
    val totalQuestions: Int get() = subjects.sumOf { it.questionCount }
    val examLabel: String get() = Labels.exam(selectedExam)

    /** Subjects grouped as on the board routine: compulsory first, then each group. */
    val subjectSections: List<SubjectSection>
        get() = StudyGroup.entries.mapNotNull { group ->
            subjects.filter { it.group == group }.takeIf { it.isNotEmpty() }?.let { SubjectSection(Labels.group(group), it) }
        }

    val finderSubject: Subject? get() = finderSubjectId?.let(catalog::subject)
    val finderBoard: Board? get() = finderBoardId?.let(catalog::board)

    /** The paper picked in "find a board question", or null until all three are chosen. */
    val finderKey: PaperKey?
        get() {
            val subject = finderSubjectId ?: return null
            val board = finderBoardId ?: return null
            val year = finderYear ?: return null
            return PaperKey(subject, board, year)
        }

    val finderNote: String? get() = finderYear?.let { catalog.noteFor(selectedExam, it)?.text }
}

sealed class HomeIntent {
    data object Retry : HomeIntent()
    data class SelectExam(val exam: ExamType) : HomeIntent()
    data class SelectFinderSubject(val subjectId: String) : HomeIntent()
    data class SelectFinderBoard(val boardId: String) : HomeIntent()
    data class SelectFinderYear(val year: Int) : HomeIntent()
}

sealed class HomeEffect {
    data class ShowMessage(val message: String) : HomeEffect()
}

class HomeStore(
    private val initializeApp: InitializeAppUseCase,
    private val getCatalog: GetCatalogUseCase,
    private val getStats: GetStatsUseCase,
    dispatcher: CoroutineDispatcher,
) : BaseStore<HomeState, HomeIntent, HomeEffect>(HomeState(), dispatcher) {

    private var loadJob: Job? = null

    init {
        load()
    }

    override fun dispatch(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Retry -> load()
            is HomeIntent.SelectExam -> setState { withExam(intent.exam) }
            is HomeIntent.SelectFinderSubject -> setState { copy(finderSubjectId = intent.subjectId) }
            is HomeIntent.SelectFinderBoard -> setState { copy(finderBoardId = intent.boardId) }
            is HomeIntent.SelectFinderYear -> setState { copy(finderYear = intent.year) }
        }
    }

    private fun load() {
        loadJob?.cancel()
        setState { copy(isLoading = true, errorMessage = null) }
        loadJob = scope.launch {
            val profile = initializeApp().getOrElse { error ->
                setState { copy(isLoading = false, errorMessage = error.message ?: GENERIC_ERROR) }
                return@launch
            }
            val catalog = getCatalog().getOrElse { error ->
                setState { copy(isLoading = false, errorMessage = error.message ?: GENERIC_ERROR) }
                return@launch
            }
            setState { copy(isLoading = false, profileName = profile.displayName, catalog = catalog).withExam(selectedExam) }

            getStats()
                .catch { error -> emit(HomeEffect.ShowMessage(error.message ?: GENERIC_ERROR)) }
                .collect { stats -> setState { copy(stats = stats) } }
        }
    }

    /** Switches exam and resets the paper finder to sensible defaults (first subject, first board, latest year). */
    private fun HomeState.withExam(exam: ExamType): HomeState {
        val next = copy(selectedExam = exam)
        val subjects = next.subjects
        return next.copy(
            finderSubjectId = finderSubjectId?.takeIf { id -> subjects.any { it.id == id } } ?: subjects.firstOrNull()?.id,
            finderBoardId = finderBoardId ?: next.boards.firstOrNull()?.id,
            finderYear = finderYear?.takeIf { it in next.years } ?: next.years.firstOrNull(),
        )
    }

    private companion object {
        const val GENERIC_ERROR = "প্রশ্নব্যাংক লোড করা যায়নি। আবার চেষ্টা করুন।"
    }
}
