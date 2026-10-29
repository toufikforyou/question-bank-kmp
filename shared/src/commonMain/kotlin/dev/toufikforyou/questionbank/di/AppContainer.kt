package dev.toufikforyou.questionbank.di

import dev.toufikforyou.questionbank.data.db.QuestionBankDatabase
import dev.toufikforyou.questionbank.data.local.DatabaseDriverFactory
import dev.toufikforyou.questionbank.data.repository.SqlDelightAttemptRepository
import dev.toufikforyou.questionbank.data.repository.SqlDelightProfileRepository
import dev.toufikforyou.questionbank.data.repository.SqlDelightQuestionRepository
import dev.toufikforyou.questionbank.data.seed.SeedReader
import dev.toufikforyou.questionbank.domain.model.SystemTimeProvider
import dev.toufikforyou.questionbank.domain.model.TimeProvider
import dev.toufikforyou.questionbank.domain.repository.AttemptRepository
import dev.toufikforyou.questionbank.domain.repository.ProfileRepository
import dev.toufikforyou.questionbank.domain.repository.QuestionRepository
import dev.toufikforyou.questionbank.domain.usecase.AnswerQuestionUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetBookmarkedQuestionsUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetCategoriesUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetQuestionsUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetStatsUseCase
import dev.toufikforyou.questionbank.domain.usecase.InitializeAppUseCase
import dev.toufikforyou.questionbank.domain.usecase.ToggleBookmarkUseCase
import dev.toufikforyou.questionbank.presentation.bookmarks.BookmarksStore
import dev.toufikforyou.questionbank.presentation.home.HomeStore
import dev.toufikforyou.questionbank.presentation.quiz.QuizStore
import dev.toufikforyou.questionbank.presentation.stats.StatsStore
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
    private val getCategories by lazy { GetCategoriesUseCase(questionRepository) }
    private val getQuestions by lazy { GetQuestionsUseCase(questionRepository) }
    private val answerQuestion by lazy { AnswerQuestionUseCase(questionRepository, attemptRepository, timeProvider) }
    private val toggleBookmark by lazy { ToggleBookmarkUseCase(questionRepository) }
    private val getBookmarked by lazy { GetBookmarkedQuestionsUseCase(questionRepository) }
    private val getStats by lazy { GetStatsUseCase(attemptRepository, questionRepository) }

    fun homeStore(): HomeStore = HomeStore(initializeApp, getCategories, getStats, mainDispatcher)

    fun quizStore(): QuizStore = QuizStore(getQuestions, answerQuestion, toggleBookmark, mainDispatcher)

    fun bookmarksStore(): BookmarksStore = BookmarksStore(getBookmarked, toggleBookmark, mainDispatcher)

    fun statsStore(): StatsStore = StatsStore(getStats, mainDispatcher)
}
