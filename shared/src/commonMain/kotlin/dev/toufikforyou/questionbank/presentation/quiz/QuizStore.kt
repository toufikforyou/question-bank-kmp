package dev.toufikforyou.questionbank.presentation.quiz

import dev.toufikforyou.questionbank.domain.model.DomainError
import dev.toufikforyou.questionbank.domain.model.QuizRequest
import dev.toufikforyou.questionbank.domain.model.QuizSession
import dev.toufikforyou.questionbank.domain.usecase.AnswerQuestionUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetQuestionsUseCase
import dev.toufikforyou.questionbank.domain.usecase.ToggleBookmarkUseCase
import dev.toufikforyou.questionbank.presentation.mvi.BaseStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class QuizStore(
    private val getQuestions: GetQuestionsUseCase,
    private val answerQuestion: AnswerQuestionUseCase,
    private val toggleBookmark: ToggleBookmarkUseCase,
    dispatcher: CoroutineDispatcher,
) : BaseStore<QuizState, QuizIntent, QuizEffect>(QuizState(), dispatcher) {

    private var lastRequest: QuizRequest? = null
    private var startJob: Job? = null

    override fun dispatch(intent: QuizIntent) {
        when (intent) {
            is QuizIntent.Start -> start(QuizRequest(intent.categoryId, intent.limit, intent.bookmarkedOnly))
            is QuizIntent.SelectOption -> select(intent.optionId)
            QuizIntent.Next -> next()
            QuizIntent.ToggleBookmark -> onToggleBookmark()
            QuizIntent.Restart -> lastRequest?.let(::start)
        }
    }

    private fun start(request: QuizRequest) {
        lastRequest = request
        startJob?.cancel()
        setState { QuizState(status = QuizStatus.LOADING) }
        startJob = scope.launch {
            getQuestions(request).fold(
                onSuccess = { questions ->
                    setState { QuizState(status = QuizStatus.IN_PROGRESS, session = QuizSession(questions)) }
                },
                onFailure = { error ->
                    val status = if (error is DomainError.EmptyQuestionBank) QuizStatus.EMPTY else QuizStatus.ERROR
                    setState { QuizState(status = status, errorMessage = error.message) }
                },
            )
        }
    }

    private fun select(optionId: String) {
        val snapshot = currentState
        val question = snapshot.currentQuestion ?: return
        // Ignore double taps and taps after the answer is locked in.
        if (snapshot.selectedOptionId != null || !question.hasOption(optionId)) return

        setState { copy(selectedOptionId = optionId) }
        scope.launch {
            answerQuestion(question.id, optionId).fold(
                onSuccess = { result ->
                    // The user may have restarted while the write was in flight.
                    if (currentState.currentQuestion?.id != question.id) return@fold
                    setState { copy(lastAnswer = result, session = session?.record(result)) }
                    emit(QuizEffect.Haptic(if (result.isCorrect) HapticKind.CORRECT else HapticKind.INCORRECT))
                },
                onFailure = { error ->
                    setState { copy(selectedOptionId = null) }
                    emit(QuizEffect.ShowMessage(error.message ?: "Your answer could not be saved."))
                },
            )
        }
    }

    private fun next() {
        val snapshot = currentState
        val session = snapshot.session ?: return
        if (snapshot.status != QuizStatus.IN_PROGRESS || snapshot.lastAnswer == null) return

        val advanced = session.advance()
        if (advanced.isFinished) {
            setState { copy(status = QuizStatus.FINISHED, session = advanced, selectedOptionId = null, lastAnswer = null) }
            emit(QuizEffect.Haptic(HapticKind.COMPLETED))
        } else {
            setState { copy(session = advanced, selectedOptionId = null, lastAnswer = null) }
        }
    }

    private fun onToggleBookmark() {
        val question = currentState.currentQuestion ?: return
        scope.launch {
            toggleBookmark(question.id).fold(
                onSuccess = { bookmarked ->
                    setState { copy(session = session?.updateQuestion(question.id) { it.copy(isBookmarked = bookmarked) }) }
                    emit(QuizEffect.Haptic(HapticKind.SELECTION))
                },
                onFailure = { error -> emit(QuizEffect.ShowMessage(error.message ?: "Bookmark could not be updated.")) },
            )
        }
    }
}
