package dev.toufikforyou.questionbank.presentation.questions

import dev.toufikforyou.questionbank.domain.model.AnswerResult
import dev.toufikforyou.questionbank.domain.model.Appearance
import dev.toufikforyou.questionbank.domain.model.Catalog
import dev.toufikforyou.questionbank.domain.model.ExamType
import dev.toufikforyou.questionbank.domain.model.PaperKey
import dev.toufikforyou.questionbank.domain.model.Question
import dev.toufikforyou.questionbank.domain.model.QuestionType
import dev.toufikforyou.questionbank.domain.model.QuizRequest
import dev.toufikforyou.questionbank.domain.usecase.AnswerQuestionUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetBookmarkedQuestionsUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetCatalogUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetChapterQuestionsUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetPaperUseCase
import dev.toufikforyou.questionbank.domain.usecase.SearchQuestionsUseCase
import dev.toufikforyou.questionbank.domain.usecase.ToggleBookmarkUseCase
import dev.toufikforyou.questionbank.presentation.format.Labels
import dev.toufikforyou.questionbank.presentation.mvi.BaseStore
import dev.toufikforyou.questionbank.presentation.quiz.HapticKind
import dev.toufikforyou.questionbank.presentation.quiz.OptionState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** What a question list shows. The same screen serves papers, chapters, search and bookmarks. */
sealed class QuestionListSource {
    data class Paper(val key: PaperKey) : QuestionListSource()
    data class Chapter(val chapterId: String) : QuestionListSource()
    data class Search(val exam: ExamType) : QuestionListSource()
    data object Bookmarks : QuestionListSource()
}

enum class QuestionFilter { ALL, MCQ, CQ }

/** A question plus everything the list needs to render it. */
data class QuestionItem(
    val question: Question,
    /** Position label: "১" in a paper, otherwise the running number. */
    val number: String,
    val subjectName: String,
    val chapterName: String,
    val tags: List<String>,
    val repeatLabel: String?,
)

data class QuestionListState(
    val source: QuestionListSource? = null,
    val isLoading: Boolean = true,
    val title: String = "",
    val subtitle: String = "",
    val items: List<QuestionItem> = emptyList(),
    val filter: QuestionFilter = QuestionFilter.ALL,
    val query: String = "",
    /** questionId -> chosen option for MCQs answered inline. */
    val selections: Map<String, String> = emptyMap(),
    val answers: Map<String, AnswerResult> = emptyMap(),
    /** Questions whose answer/model answer is shown. */
    val revealed: Set<String> = emptySet(),
    val errorMessage: String? = null,
) {
    val isSearch: Boolean get() = source is QuestionListSource.Search
    val isBookmarks: Boolean get() = source is QuestionListSource.Bookmarks

    val visibleItems: List<QuestionItem>
        get() = when (filter) {
            QuestionFilter.ALL -> items
            QuestionFilter.MCQ -> items.filter { it.question.type == QuestionType.MCQ }
            QuestionFilter.CQ -> items.filter { it.question.type == QuestionType.CQ }
        }

    val mcqCount: Int get() = items.count { it.question.isMcq }
    val cqCount: Int get() = items.size - mcqCount
    val isEmpty: Boolean get() = !isLoading && errorMessage == null && items.isEmpty()

    /** The list has questions, but none of the selected type. */
    val isFilteredEmpty: Boolean get() = !isLoading && items.isNotEmpty() && visibleItems.isEmpty()

    val filteredEmptyMessage: String
        get() = if (filter == QuestionFilter.CQ) "এখানে কোনো সৃজনশীল প্রশ্ন নেই।" else "এখানে কোনো বহুনির্বাচনি প্রশ্ন নেই।"

    val emptyMessage: String
        get() = when (source) {
            is QuestionListSource.Search -> if (query.isBlank()) "প্রশ্নের যেকোনো শব্দ লিখে খুঁজুন।" else "কোনো প্রশ্ন পাওয়া যায়নি।"
            QuestionListSource.Bookmarks -> "এখনো কোনো প্রশ্ন সংরক্ষণ করা হয়নি। প্রশ্নের পাশের বুকমার্ক আইকনে চাপুন।"
            else -> "এখানে কোনো প্রশ্ন নেই।"
        }

    /** MCQ practice for this list, or null when there is nothing to practise. */
    val practiceRequest: QuizRequest?
        get() {
            if (mcqCount == 0) return null
            return when (val s = source) {
                is QuestionListSource.Paper -> QuizRequest.forPaper(s.key)
                is QuestionListSource.Chapter -> QuizRequest.forChapter(s.chapterId)
                QuestionListSource.Bookmarks -> QuizRequest.forBookmarks()
                else -> null
            }
        }

    val practiceLabel: String
        get() = if (source is QuestionListSource.Paper) "পরীক্ষা দিন (${Labels.number(mcqCount)} মিনিট)" else "বহুনির্বাচনি অনুশীলন"

    fun isRevealed(questionId: String): Boolean = questionId in revealed

    fun selectedOption(questionId: String): String? = selections[questionId]

    fun optionState(questionId: String, optionId: String): OptionState {
        val answer = answers[questionId]
        val item = items.firstOrNull { it.question.id == questionId }
        if (answer == null) {
            if (questionId in revealed && item?.question?.correctOptionId == optionId) return OptionState.CORRECT
            return if (selections[questionId] == optionId) OptionState.SELECTED else OptionState.IDLE
        }
        return when (optionId) {
            answer.correctOptionId -> OptionState.CORRECT
            answer.selectedOptionId -> OptionState.INCORRECT
            else -> OptionState.DIMMED
        }
    }
}

sealed class QuestionListIntent {
    data class SetFilter(val filter: QuestionFilter) : QuestionListIntent()
    data class UpdateQuery(val query: String) : QuestionListIntent()
    data class SelectOption(val questionId: String, val optionId: String) : QuestionListIntent()
    data class ToggleReveal(val questionId: String) : QuestionListIntent()
    data class ToggleBookmark(val questionId: String) : QuestionListIntent()
    data object Retry : QuestionListIntent()
}

sealed class QuestionListEffect {
    data class Haptic(val kind: HapticKind) : QuestionListEffect()
    data class ShowMessage(val message: String) : QuestionListEffect()
}

class QuestionListStore(
    private val source: QuestionListSource,
    private val getCatalog: GetCatalogUseCase,
    private val getPaper: GetPaperUseCase,
    private val getChapterQuestions: GetChapterQuestionsUseCase,
    private val searchQuestions: SearchQuestionsUseCase,
    private val getBookmarked: GetBookmarkedQuestionsUseCase,
    private val answerQuestion: AnswerQuestionUseCase,
    private val toggleBookmark: ToggleBookmarkUseCase,
    dispatcher: CoroutineDispatcher,
) : BaseStore<QuestionListState, QuestionListIntent, QuestionListEffect>(QuestionListState(source = source), dispatcher) {

    private var catalog: Catalog = Catalog.EMPTY
    private var loadJob: Job? = null

    init {
        load()
    }

    override fun dispatch(intent: QuestionListIntent) {
        when (intent) {
            is QuestionListIntent.SetFilter -> setState { copy(filter = intent.filter) }
            is QuestionListIntent.UpdateQuery -> onQuery(intent.query)
            is QuestionListIntent.SelectOption -> onSelect(intent.questionId, intent.optionId)
            is QuestionListIntent.ToggleReveal -> setState {
                copy(revealed = if (intent.questionId in revealed) revealed - intent.questionId else revealed + intent.questionId)
            }
            is QuestionListIntent.ToggleBookmark -> onToggleBookmark(intent.questionId)
            QuestionListIntent.Retry -> load()
        }
    }

    private fun load() {
        loadJob?.cancel()
        setState { copy(isLoading = source !is QuestionListSource.Search, errorMessage = null) }
        loadJob = scope.launch {
            catalog = getCatalog().getOrElse { return@launch fail(it) }
            when (source) {
                is QuestionListSource.Paper -> {
                    val paper = getPaper(source.key).getOrElse { return@launch fail(it, titleFor(source)) }
                    show(titleFor(source), paper.subject.name, paper.questions, numbered = true)
                }
                is QuestionListSource.Chapter -> {
                    val questions = getChapterQuestions(source.chapterId).getOrElse { return@launch fail(it) }
                    val chapter = catalog.chapter(source.chapterId)
                    show(
                        title = chapter?.let { Labels.chapter(it.number, it.name) }.orEmpty(),
                        subtitle = chapter?.let { catalog.subject(it.subjectId)?.name }.orEmpty(),
                        questions = questions,
                        numbered = false,
                    )
                }
                is QuestionListSource.Search -> {
                    val examLabel = Labels.exam(source.exam)
                    setState { copy(isLoading = false, title = "প্রশ্ন খুঁজুন", subtitle = examLabel) }
                }
                QuestionListSource.Bookmarks -> getBookmarked().collect { questions ->
                    show("সংরক্ষিত প্রশ্ন", "${Labels.number(questions.size)}টি প্রশ্ন", questions, numbered = false)
                }
            }
        }
    }

    private fun onQuery(query: String) {
        val exam = (source as? QuestionListSource.Search)?.exam ?: return
        setState { copy(query = query) }
        loadJob?.cancel()
        if (SearchQuestionsUseCase.normalize(query).length < SearchQuestionsUseCase.MIN_QUERY_LENGTH) {
            setState { copy(items = emptyList(), isLoading = false, errorMessage = null) }
            return
        }
        loadJob = scope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            setState { copy(isLoading = true) }
            if (catalog.subjects.isEmpty()) catalog = getCatalog().getOrElse { return@launch fail(it) }
            val results = searchQuestions(query, exam).getOrElse { return@launch fail(it) }
            show(currentState.title, "${Labels.number(results.size)}টি ফলাফল", results, numbered = false)
        }
    }

    private fun onSelect(questionId: String, optionId: String) {
        val snapshot = currentState
        val question = snapshot.items.firstOrNull { it.question.id == questionId }?.question ?: return
        if (!question.isMcq || questionId in snapshot.selections || questionId in snapshot.answers) return

        setState { copy(selections = selections + (questionId to optionId)) }
        scope.launch {
            answerQuestion(questionId, optionId).fold(
                onSuccess = { result ->
                    setState { copy(answers = answers + (questionId to result), revealed = revealed + questionId) }
                    emit(QuestionListEffect.Haptic(if (result.isCorrect) HapticKind.CORRECT else HapticKind.INCORRECT))
                },
                onFailure = { error ->
                    setState { copy(selections = selections - questionId) }
                    emit(QuestionListEffect.ShowMessage(error.message ?: "উত্তর সংরক্ষণ করা যায়নি।"))
                },
            )
        }
    }

    private fun onToggleBookmark(questionId: String) {
        scope.launch {
            toggleBookmark(questionId).fold(
                onSuccess = { bookmarked ->
                    if (source !is QuestionListSource.Bookmarks) {
                        setState {
                            copy(items = items.map {
                                if (it.question.id == questionId) it.copy(question = it.question.copy(isBookmarked = bookmarked)) else it
                            })
                        }
                    }
                    emit(QuestionListEffect.Haptic(HapticKind.SELECTION))
                },
                onFailure = { emit(QuestionListEffect.ShowMessage(it.message ?: "বুকমার্ক পরিবর্তন করা যায়নি।")) },
            )
        }
    }

    private fun show(title: String, subtitle: String, questions: List<Question>, numbered: Boolean) {
        val items = questions.mapIndexed { index, question ->
            val chapter = catalog.chapter(question.chapterId)
            QuestionItem(
                question = question,
                number = Labels.number(if (numbered) question.appearances.firstOrNull(::matchesSource)?.number ?: (index + 1) else index + 1),
                subjectName = catalog.subject(question.subjectId)?.name.orEmpty(),
                chapterName = chapter?.let { Labels.chapter(it.number, it.name) }.orEmpty(),
                tags = Labels.appearanceTags(catalog, question.appearances),
                repeatLabel = Labels.repeat(question.repeatCount),
            )
        }
        setState { copy(isLoading = false, title = title, subtitle = subtitle, items = items, errorMessage = null) }
    }

    private fun matchesSource(appearance: Appearance): Boolean {
        val key = (source as? QuestionListSource.Paper)?.key ?: return false
        return appearance.boardId == key.boardId && appearance.year == key.year
    }

    private fun titleFor(source: QuestionListSource.Paper): String =
        Labels.boardYear(catalog.board(source.key.boardId)?.name ?: source.key.boardId, source.key.year)

    private fun fail(error: Throwable, title: String = currentState.title) {
        setState { copy(isLoading = false, title = title, errorMessage = error.message ?: "প্রশ্ন লোড করা যায়নি।") }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
    }
}
