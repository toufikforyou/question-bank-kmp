package dev.toufikforyou.questionbank.presentation

import app.cash.turbine.test
import dev.toufikforyou.questionbank.domain.model.PaperKey
import dev.toufikforyou.questionbank.domain.model.QuizRequest
import dev.toufikforyou.questionbank.domain.usecase.AnswerQuestionUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetQuestionsUseCase
import dev.toufikforyou.questionbank.domain.usecase.ToggleBookmarkUseCase
import dev.toufikforyou.questionbank.fakes.FakeAttemptRepository
import dev.toufikforyou.questionbank.fakes.FakeQuestionRepository
import dev.toufikforyou.questionbank.fakes.FixedTimeProvider
import dev.toufikforyou.questionbank.fakes.PHYSICS
import dev.toufikforyou.questionbank.fakes.at
import dev.toufikforyou.questionbank.fakes.mcq
import dev.toufikforyou.questionbank.presentation.quiz.HapticKind
import dev.toufikforyou.questionbank.presentation.quiz.OptionState
import dev.toufikforyou.questionbank.presentation.quiz.QuizEffect
import dev.toufikforyou.questionbank.presentation.quiz.QuizIntent
import dev.toufikforyou.questionbank.presentation.quiz.QuizStatus
import dev.toufikforyou.questionbank.presentation.quiz.QuizStore
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuizStoreTest {

    private val attempts = FakeAttemptRepository()

    private fun TestScope.createStore(repo: FakeQuestionRepository, unconfined: Boolean = true) = QuizStore(
        getQuestions = GetQuestionsUseCase(repo, Random(7)),
        answerQuestion = AnswerQuestionUseCase(repo, attempts, FixedTimeProvider()),
        toggleBookmark = ToggleBookmarkUseCase(repo),
        dispatcher = if (unconfined) UnconfinedTestDispatcher(testScheduler) else StandardTestDispatcher(testScheduler),
    )

    private val practice = QuizIntent.Start(QuizRequest.forSubject(PHYSICS))

    @Test
    fun startWithEmptyBankShowsEmptyState() = runTest {
        val store = createStore(FakeQuestionRepository())

        store.dispatch(practice)

        assertEquals(QuizStatus.EMPTY, store.currentState.status)
        assertNull(store.currentState.currentQuestion)
        store.close()
    }

    @Test
    fun answeringCorrectlyRevealsAnswerAndFiresSuccessHaptic() = runTest {
        val store = createStore(FakeQuestionRepository(listOf(mcq("q1", correctOptionId = "খ"))))
        store.dispatch(practice)

        store.effects.test {
            store.dispatch(QuizIntent.SelectOption("খ"))

            assertEquals(QuizEffect.Haptic(HapticKind.CORRECT), awaitItem())
            assertEquals(OptionState.CORRECT, store.currentState.optionState("খ"))
            assertEquals(OptionState.DIMMED, store.currentState.optionState("ক"))
        }
        store.close()
    }

    @Test
    fun wrongAnswerMarksSelectionIncorrect() = runTest {
        val store = createStore(FakeQuestionRepository(listOf(mcq("q1", correctOptionId = "খ"))))
        store.dispatch(practice)

        store.effects.test {
            store.dispatch(QuizIntent.SelectOption("ক"))

            assertEquals(QuizEffect.Haptic(HapticKind.INCORRECT), awaitItem())
            assertEquals(OptionState.INCORRECT, store.currentState.optionState("ক"))
        }
        store.close()
    }

    @Test
    fun doubleTapRecordsOnlyOneAttempt() = runTest {
        val store = createStore(FakeQuestionRepository(listOf(mcq("q1"))))
        store.dispatch(practice)

        store.dispatch(QuizIntent.SelectOption("ক"))
        store.dispatch(QuizIntent.SelectOption("খ"))

        assertEquals(1, attempts.recorded.size)
        store.close()
    }

    @Test
    fun completingAllQuestionsFinishesWithBanglaScore() = runTest {
        val store = createStore(FakeQuestionRepository((1..3).map { mcq("q$it") }))
        store.dispatch(practice)

        listOf("ক", "খ", "ক").forEach { option ->
            store.dispatch(QuizIntent.SelectOption(option))
            store.dispatch(QuizIntent.Next)
        }

        val finished = store.currentState
        assertEquals(QuizStatus.FINISHED, finished.status)
        assertEquals(2, finished.score.correct)
        assertEquals("৩টির মধ্যে ২টি সঠিক", finished.scoreLabel)
        assertEquals("৬৭%", finished.percentLabel)
        store.close()
    }

    @Test
    fun paperExamModeCountsDownAndFinishesWhenTimeRunsOut() = runTest {
        val repo = FakeQuestionRepository(listOf(mcq("q1", appearances = listOf(at("dhaka", 2023, 1))), mcq("q2", appearances = listOf(at("dhaka", 2023, 2)))))
        val store = createStore(repo, unconfined = false)

        store.dispatch(QuizIntent.Start(QuizRequest.forPaper(PaperKey(PHYSICS, "dhaka", 2023))))
        runCurrent()
        assertEquals(120, store.currentState.remainingSeconds)
        assertEquals("০২:০০", store.currentState.timerLabel)

        advanceTimeBy(61_000)
        runCurrent()
        assertEquals(59, store.currentState.remainingSeconds)
        assertTrue(store.currentState.isTimeRunningOut)

        store.dispatch(QuizIntent.SelectOption("ক"))
        runCurrent()
        advanceTimeBy(60_000)
        runCurrent()

        val finished = store.currentState
        assertEquals(QuizStatus.FINISHED, finished.status)
        assertTrue(finished.timeUp)
        assertEquals(1, finished.score.correct)
        assertEquals(2, finished.score.total)
        store.close()
    }

    @Test
    fun untimedPracticeHasNoClock() = runTest {
        val store = createStore(FakeQuestionRepository(listOf(mcq("q1"))))

        store.dispatch(practice)

        assertNull(store.currentState.timerLabel)
        store.close()
    }

    @Test
    fun restartBuildsAFreshSession() = runTest {
        val store = createStore(FakeQuestionRepository(listOf(mcq("q1"))))
        store.dispatch(practice)
        store.dispatch(QuizIntent.SelectOption("ক"))
        store.dispatch(QuizIntent.Next)

        store.dispatch(QuizIntent.Restart)

        assertEquals(QuizStatus.IN_PROGRESS, store.currentState.status)
        assertEquals(0, store.currentState.score.correct)
        store.close()
    }
}
