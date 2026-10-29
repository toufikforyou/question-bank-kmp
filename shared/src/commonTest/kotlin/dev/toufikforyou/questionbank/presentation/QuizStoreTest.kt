package dev.toufikforyou.questionbank.presentation

import app.cash.turbine.test
import dev.toufikforyou.questionbank.domain.usecase.AnswerQuestionUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetQuestionsUseCase
import dev.toufikforyou.questionbank.domain.usecase.ToggleBookmarkUseCase
import dev.toufikforyou.questionbank.fakes.FakeAttemptRepository
import dev.toufikforyou.questionbank.fakes.FakeQuestionRepository
import dev.toufikforyou.questionbank.fakes.FixedTimeProvider
import dev.toufikforyou.questionbank.fakes.question
import dev.toufikforyou.questionbank.presentation.quiz.HapticKind
import dev.toufikforyou.questionbank.presentation.quiz.OptionState
import dev.toufikforyou.questionbank.presentation.quiz.QuizEffect
import dev.toufikforyou.questionbank.presentation.quiz.QuizIntent
import dev.toufikforyou.questionbank.presentation.quiz.QuizStatus
import dev.toufikforyou.questionbank.presentation.quiz.QuizStore
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuizStoreTest {

    private val attempts = FakeAttemptRepository()

    private fun TestScope.createStore(repo: FakeQuestionRepository): QuizStore {
        val time = FixedTimeProvider()
        return QuizStore(
            getQuestions = GetQuestionsUseCase(repo, Random(7)),
            answerQuestion = AnswerQuestionUseCase(repo, attempts, time),
            toggleBookmark = ToggleBookmarkUseCase(repo),
            dispatcher = UnconfinedTestDispatcher(testScheduler),
        )
    }

    private val start = QuizIntent.Start(categoryId = null, limit = 10, bookmarkedOnly = false)

    @Test
    fun startWithEmptyBankShowsEmptyState() = runTest {
        val store = createStore(FakeQuestionRepository())

        store.dispatch(start)

        assertEquals(QuizStatus.EMPTY, store.currentState.status)
        assertNull(store.currentState.currentQuestion)
        store.close()
    }

    @Test
    fun answeringCorrectlyRevealsAnswerAndFiresSuccessHaptic() = runTest {
        val store = createStore(FakeQuestionRepository(listOf(question("q1", correctOptionId = "b"))))
        store.dispatch(start)

        store.effects.test {
            store.dispatch(QuizIntent.SelectOption("b"))

            assertEquals(QuizEffect.Haptic(HapticKind.CORRECT), awaitItem())
            val state = store.currentState
            assertTrue(state.isAnswerRevealed)
            assertEquals(OptionState.CORRECT, state.optionState("b"))
            assertEquals(OptionState.DIMMED, state.optionState("a"))
            assertEquals(1f, state.progress)
        }
        store.close()
    }

    @Test
    fun wrongAnswerMarksSelectionIncorrectAndFiresErrorHaptic() = runTest {
        val store = createStore(FakeQuestionRepository(listOf(question("q1", correctOptionId = "b"))))
        store.dispatch(start)

        store.effects.test {
            store.dispatch(QuizIntent.SelectOption("a"))

            assertEquals(QuizEffect.Haptic(HapticKind.INCORRECT), awaitItem())
            assertEquals(OptionState.INCORRECT, store.currentState.optionState("a"))
            assertEquals(OptionState.CORRECT, store.currentState.optionState("b"))
        }
        store.close()
    }

    @Test
    fun doubleTapRecordsOnlyOneAttempt() = runTest {
        val store = createStore(FakeQuestionRepository(listOf(question("q1"))))
        store.dispatch(start)

        store.dispatch(QuizIntent.SelectOption("a"))
        store.dispatch(QuizIntent.SelectOption("b"))

        assertEquals(1, attempts.recorded.size)
        assertEquals("a", store.currentState.lastAnswer?.selectedOptionId)
        store.close()
    }

    @Test
    fun nextIsIgnoredUntilAnswered() = runTest {
        val store = createStore(FakeQuestionRepository(listOf(question("q1"), question("q2"))))
        store.dispatch(start)

        store.dispatch(QuizIntent.Next)

        assertEquals(1, store.currentState.questionNumber)
        store.close()
    }

    @Test
    fun completingAllQuestionsFinishesWithCorrectScore() = runTest {
        val store = createStore(FakeQuestionRepository((1..3).map { question("q$it", correctOptionId = "a") }))
        store.dispatch(start)

        store.state.test {
            skipItems(1)
            listOf("a", "b", "a").forEach { option ->
                store.dispatch(QuizIntent.SelectOption(option))
                store.dispatch(QuizIntent.Next)
            }
            val finished = expectMostRecentItem()
            assertEquals(QuizStatus.FINISHED, finished.status)
            assertEquals(2, finished.score.correct)
            assertEquals(3, finished.score.total)
            assertEquals(67, finished.score.percent)
        }
        store.close()
    }

    @Test
    fun toggleBookmarkUpdatesCurrentQuestion() = runTest {
        val store = createStore(FakeQuestionRepository(listOf(question("q1"))))
        store.dispatch(start)

        store.dispatch(QuizIntent.ToggleBookmark)

        assertEquals(true, store.currentState.currentQuestion?.isBookmarked)
        store.close()
    }

    @Test
    fun restartBuildsAFreshSession() = runTest {
        val store = createStore(FakeQuestionRepository(listOf(question("q1"))))
        store.dispatch(start)
        store.dispatch(QuizIntent.SelectOption("a"))
        store.dispatch(QuizIntent.Next)
        assertEquals(QuizStatus.FINISHED, store.currentState.status)

        store.dispatch(QuizIntent.Restart)

        assertEquals(QuizStatus.IN_PROGRESS, store.currentState.status)
        assertEquals(0, store.currentState.score.correct)
        store.close()
    }
}
