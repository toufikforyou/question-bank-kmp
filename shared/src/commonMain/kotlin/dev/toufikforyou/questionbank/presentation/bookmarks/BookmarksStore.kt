package dev.toufikforyou.questionbank.presentation.bookmarks

import dev.toufikforyou.questionbank.domain.model.Question
import dev.toufikforyou.questionbank.domain.usecase.GetBookmarkedQuestionsUseCase
import dev.toufikforyou.questionbank.domain.usecase.ToggleBookmarkUseCase
import dev.toufikforyou.questionbank.presentation.mvi.BaseStore
import dev.toufikforyou.questionbank.presentation.quiz.HapticKind
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

data class BookmarksState(
    val isLoading: Boolean = true,
    val questions: List<Question> = emptyList(),
) {
    val isEmpty: Boolean get() = !isLoading && questions.isEmpty()
}

sealed class BookmarksIntent {
    data class Remove(val questionId: String) : BookmarksIntent()
}

sealed class BookmarksEffect {
    data class Haptic(val kind: HapticKind) : BookmarksEffect()
    data class ShowMessage(val message: String) : BookmarksEffect()
}

class BookmarksStore(
    getBookmarkedQuestions: GetBookmarkedQuestionsUseCase,
    private val toggleBookmark: ToggleBookmarkUseCase,
    dispatcher: CoroutineDispatcher,
) : BaseStore<BookmarksState, BookmarksIntent, BookmarksEffect>(BookmarksState(), dispatcher) {

    init {
        scope.launch {
            getBookmarkedQuestions().collect { questions ->
                setState { copy(isLoading = false, questions = questions) }
            }
        }
    }

    override fun dispatch(intent: BookmarksIntent) {
        when (intent) {
            is BookmarksIntent.Remove -> scope.launch {
                toggleBookmark(intent.questionId)
                    .onSuccess { emit(BookmarksEffect.Haptic(HapticKind.SELECTION)) }
                    .onFailure { emit(BookmarksEffect.ShowMessage(it.message ?: "Bookmark could not be removed.")) }
            }
        }
    }
}
