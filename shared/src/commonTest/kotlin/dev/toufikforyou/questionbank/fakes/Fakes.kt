package dev.toufikforyou.questionbank.fakes

import dev.toufikforyou.questionbank.domain.model.AnswerOption
import dev.toufikforyou.questionbank.domain.model.AnswerRecord
import dev.toufikforyou.questionbank.domain.model.Category
import dev.toufikforyou.questionbank.domain.model.Difficulty
import dev.toufikforyou.questionbank.domain.model.Question
import dev.toufikforyou.questionbank.domain.model.TimeProvider
import dev.toufikforyou.questionbank.domain.model.UserProfile
import dev.toufikforyou.questionbank.domain.repository.AttemptRepository
import dev.toufikforyou.questionbank.domain.repository.ProfileRepository
import dev.toufikforyou.questionbank.domain.repository.QuestionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

fun question(
    id: String,
    categoryId: String = "general",
    correctOptionId: String = "a",
    bookmarked: Boolean = false,
) = Question(
    id = id,
    categoryId = categoryId,
    prompt = "Prompt for $id",
    options = listOf(AnswerOption("a", "Option A"), AnswerOption("b", "Option B"), AnswerOption("c", "Option C")),
    correctOptionId = correctOptionId,
    explanation = "Because $correctOptionId",
    difficulty = Difficulty.EASY,
    isBookmarked = bookmarked,
)

fun attempt(categoryId: String, isCorrect: Boolean, at: Long) = AnswerRecord(
    questionId = "q-$at",
    categoryId = categoryId,
    selectedOptionId = if (isCorrect) "a" else "b",
    isCorrect = isCorrect,
    answeredAtMillis = at,
)

class FakeQuestionRepository(
    questions: List<Question> = emptyList(),
    private val categories: List<Category> = emptyList(),
) : QuestionRepository {
    private val questions = MutableStateFlow(questions.associateBy { it.id })
    private val bookmarks = MutableStateFlow(questions.filter { it.isBookmarked }.map { it.id })

    var seedCalls = 0
        private set
    var failWith: Exception? = null

    override suspend fun ensureSeeded() {
        failWith?.let { throw it }
        seedCalls++
    }

    override fun observeCategories(): Flow<List<Category>> = MutableStateFlow(categories).asStateFlow()

    override suspend fun getQuestions(categoryId: String?): List<Question> {
        failWith?.let { throw it }
        return questions.value.values
            .filter { categoryId == null || it.categoryId == categoryId }
            .map { it.withBookmark() }
    }

    override suspend fun getQuestion(id: String): Question? = questions.value[id]?.withBookmark()

    override fun observeBookmarkedQuestions(): Flow<List<Question>> =
        bookmarks.map { ids -> ids.mapNotNull { questions.value[it]?.copy(isBookmarked = true) } }

    override suspend fun setBookmarked(questionId: String, bookmarked: Boolean) {
        bookmarks.update { ids -> (ids - questionId).let { if (bookmarked) listOf(questionId) + it else it } }
    }

    private fun Question.withBookmark() = copy(isBookmarked = id in bookmarks.value)
}

class FakeAttemptRepository(initial: List<AnswerRecord> = emptyList()) : AttemptRepository {
    private val attempts = MutableStateFlow(initial)
    val recorded: List<AnswerRecord> get() = attempts.value

    override suspend fun record(attempt: AnswerRecord) {
        attempts.update { it + attempt }
    }

    override fun observeAttempts(): Flow<List<AnswerRecord>> = attempts.asStateFlow()
}

class FakeProfileRepository : ProfileRepository {
    var created = 0
        private set
    private var profile: UserProfile? = null

    override suspend fun getOrCreateProfile(): UserProfile =
        profile ?: UserProfile("local-1", "Learner", 0L).also { profile = it; created++ }
}

class FixedTimeProvider(var now: Long = 1_000L) : TimeProvider {
    override fun nowMillis(): Long = now++
}
