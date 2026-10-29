package dev.toufikforyou.questionbank.di

import dev.toufikforyou.questionbank.data.db.QuestionBankDatabase
import dev.toufikforyou.questionbank.data.local.DatabaseDriverFactory
import dev.toufikforyou.questionbank.data.repository.SqlDelightAttemptRepository
import dev.toufikforyou.questionbank.data.repository.SqlDelightProfileRepository
import dev.toufikforyou.questionbank.data.repository.SqlDelightQuestionRepository
import dev.toufikforyou.questionbank.data.seed.SeedReader
import dev.toufikforyou.questionbank.domain.model.ExamType
import dev.toufikforyou.questionbank.domain.model.PaperKey
import dev.toufikforyou.questionbank.domain.model.SystemTimeProvider
import dev.toufikforyou.questionbank.domain.model.TimeProvider
import dev.toufikforyou.questionbank.domain.repository.AttemptRepository
import dev.toufikforyou.questionbank.domain.repository.ProfileRepository
import dev.toufikforyou.questionbank.domain.repository.QuestionRepository
import dev.toufikforyou.questionbank.domain.usecase.AnswerQuestionUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetBookmarkedQuestionsUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetCatalogUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetChapterQuestionsUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetPaperSummariesUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetPaperUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetQuestionsUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetStatsUseCase
import dev.toufikforyou.questionbank.domain.usecase.InitializeAppUseCase
import dev.toufikforyou.questionbank.domain.usecase.SearchQuestionsUseCase
import dev.toufikforyou.questionbank.domain.usecase.ToggleBookmarkUseCase
import dev.toufikforyou.questionbank.presentation.home.HomeStore
import dev.toufikforyou.questionbank.presentation.questions.QuestionListSource
import dev.toufikforyou.questionbank.presentation.questions.QuestionListStore
import dev.toufikforyou.questionbank.presentation.quiz.QuizStore
import dev.toufikforyou.questionbank.presentation.stats.StatsStore
import dev.toufikforyou.questionbank.presentation.subject.SubjectStore
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Hand-rolled dependency graph. Repositories are app-wide singletons (they own the memory cache);
 * stores are created per screen and closed by the platform when the screen goes away.
 */
class AppContainer(
    driverFactory: DatabaseDriverFactory,
    seedReader: SeedReader,
    private val ioDispatcher: CoroutineDispatcher,
    private val mainDispatcher: CoroutineDispatcher,
    private val timeProvider: TimeProvider = SystemTimeProvider,
) {
    private val database: QuestionBankDatabase by lazy { QuestionBankDatabase(driverFactory.create()) }

    val questionRepository: QuestionRepository by lazy {
        SqlDelightQuestionRepository(database, seedReader, timeProvider, ioDispatcher)
    }
    val attemptRepository: AttemptRepository by lazy { SqlDelightAttemptRepository(database, ioDispatcher) }
    val profileRepository: ProfileRepository by lazy { SqlDelightProfileRepository(database, timeProvider, ioDispatcher) }

    private val initializeApp by lazy { InitializeAppUseCase(profileRepository, questionRepository) }
    private val getCatalog by lazy { GetCatalogUseCase(questionRepository) }
    private val getPaperSummaries by lazy { GetPaperSummariesUseCase(questionRepository) }
    private val getPaper by lazy { GetPaperUseCase(questionRepository) }
    private val getChapterQuestions by lazy { GetChapterQuestionsUseCase(questionRepository) }
    private val searchQuestions by lazy { SearchQuestionsUseCase(questionRepository) }
    private val getQuestions by lazy { GetQuestionsUseCase(questionRepository) }
    private val answerQuestion by lazy { AnswerQuestionUseCase(questionRepository, attemptRepository, timeProvider) }
    private val toggleBookmark by lazy { ToggleBookmarkUseCase(questionRepository) }
    private val getBookmarked by lazy { GetBookmarkedQuestionsUseCase(questionRepository) }
    private val getStats by lazy { GetStatsUseCase(attemptRepository, questionRepository) }

    fun homeStore(): HomeStore = HomeStore(initializeApp, getCatalog, getStats, mainDispatcher)

    fun subjectStore(subjectId: String): SubjectStore =
        SubjectStore(subjectId, getCatalog, getPaperSummaries, mainDispatcher)

    fun paperStore(key: PaperKey): QuestionListStore = questionListStore(QuestionListSource.Paper(key))

    fun chapterStore(chapterId: String): QuestionListStore = questionListStore(QuestionListSource.Chapter(chapterId))

    fun searchStore(exam: ExamType): QuestionListStore = questionListStore(QuestionListSource.Search(exam))

    fun bookmarksStore(): QuestionListStore = questionListStore(QuestionListSource.Bookmarks)

    fun quizStore(): QuizStore = QuizStore(getQuestions, answerQuestion, toggleBookmark, mainDispatcher)

    fun statsStore(): StatsStore = StatsStore(getStats, mainDispatcher)

    private fun questionListStore(source: QuestionListSource): QuestionListStore = QuestionListStore(
        source, getCatalog, getPaper, getChapterQuestions, searchQuestions, getBookmarked, answerQuestion, toggleBookmark, mainDispatcher,
    )
}
