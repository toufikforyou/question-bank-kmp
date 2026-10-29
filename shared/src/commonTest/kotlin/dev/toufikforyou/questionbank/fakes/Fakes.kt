package dev.toufikforyou.questionbank.fakes

import dev.toufikforyou.questionbank.domain.model.AnswerOption
import dev.toufikforyou.questionbank.domain.model.AnswerRecord
import dev.toufikforyou.questionbank.domain.model.Appearance
import dev.toufikforyou.questionbank.domain.model.Board
import dev.toufikforyou.questionbank.domain.model.Catalog
import dev.toufikforyou.questionbank.domain.model.Chapter
import dev.toufikforyou.questionbank.domain.model.CreativePart
import dev.toufikforyou.questionbank.domain.model.Exam
import dev.toufikforyou.questionbank.domain.model.ExamType
import dev.toufikforyou.questionbank.domain.model.PaperKey
import dev.toufikforyou.questionbank.domain.model.PaperSummary
import dev.toufikforyou.questionbank.domain.model.Question
import dev.toufikforyou.questionbank.domain.model.QuestionType
import dev.toufikforyou.questionbank.domain.model.StudyGroup
import dev.toufikforyou.questionbank.domain.model.Subject
import dev.toufikforyou.questionbank.domain.model.TimeProvider
import dev.toufikforyou.questionbank.domain.model.UserProfile
import dev.toufikforyou.questionbank.domain.model.YearNote
import dev.toufikforyou.questionbank.domain.repository.AttemptRepository
import dev.toufikforyou.questionbank.domain.repository.ProfileRepository
import dev.toufikforyou.questionbank.domain.repository.QuestionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

const val PHYSICS = "ssc-physics"
const val PHYSICS_CH1 = "ssc-physics-1"
const val PHYSICS_CH2 = "ssc-physics-2"
const val HSC_ICT = "hsc-ict"

fun at(board: String, year: Int, number: Int) = Appearance(board, year, number)

fun mcq(
    id: String,
    subjectId: String = PHYSICS,
    chapterId: String = PHYSICS_CH1,
    correctOptionId: String = "ক",
    appearances: List<Appearance> = emptyList(),
    bookmarked: Boolean = false,
    prompt: String = "প্রশ্ন $id",
) = Question(
    id = id,
    subjectId = subjectId,
    chapterId = chapterId,
    type = QuestionType.MCQ,
    prompt = prompt,
    options = listOf(AnswerOption("ক", "উত্তর ক"), AnswerOption("খ", "উত্তর খ"), AnswerOption("গ", "উত্তর গ"), AnswerOption("ঘ", "উত্তর ঘ")),
    correctOptionId = correctOptionId,
    explanation = "ব্যাখ্যা $id",
    appearances = appearances,
    isBookmarked = bookmarked,
)

fun creative(
    id: String,
    subjectId: String = PHYSICS,
    chapterId: String = PHYSICS_CH1,
    appearances: List<Appearance> = emptyList(),
) = Question(
    id = id,
    subjectId = subjectId,
    chapterId = chapterId,
    type = QuestionType.CQ,
    prompt = "উদ্দীপক $id",
    parts = listOf("ক", "খ", "গ", "ঘ").mapIndexed { i, label -> CreativePart(label, "অংশ $label", i + 1, "উত্তর $label") },
    appearances = appearances,
)

fun attempt(subjectId: String, isCorrect: Boolean, at: Long) = AnswerRecord(
    questionId = "q-$at",
    subjectId = subjectId,
    selectedOptionId = if (isCorrect) "ক" else "খ",
    isCorrect = isCorrect,
    answeredAtMillis = at,
)

fun testCatalog(isSample: Boolean = true) = Catalog(
    exams = listOf(Exam(ExamType.SSC, "এসএসসি", "মাধ্যমিক"), Exam(ExamType.HSC, "এইচএসসি", "উচ্চ মাধ্যমিক")),
    boards = listOf(Board("dhaka", "ঢাকা", "Dhaka"), Board("rajshahi", "রাজশাহী", "Rajshahi")),
    subjects = listOf(
        Subject(
            PHYSICS, ExamType.SSC, "পদার্থবিজ্ঞান", "Physics", "136", StudyGroup.SCIENCE,
            listOf(Chapter(PHYSICS_CH1, PHYSICS, 1, "ভৌত রাশি", 3, 1), Chapter(PHYSICS_CH2, PHYSICS, 2, "গতি", 1, 0)),
        ),
        Subject(HSC_ICT, ExamType.HSC, "তথ্য ও যোগাযোগ প্রযুক্তি", "ICT", "275", StudyGroup.COMMON, listOf(Chapter("hsc-ict-1", HSC_ICT, 1, "সংখ্যা পদ্ধতি", 1, 0))),
    ),
    notes = listOf(YearNote(ExamType.HSC, 2020, "২০২০ সালের পরীক্ষা অনুষ্ঠিত হয়নি।")),
    yearsByExam = mapOf(ExamType.SSC to listOf(2024, 2023), ExamType.HSC to listOf(2024, 2021)),
    isSample = isSample,
)

class FakeQuestionRepository(
    questions: List<Question> = emptyList(),
    private val catalog: Catalog = testCatalog(),
) : QuestionRepository {
    private val questions = questions.associateBy { it.id }
    private val ordered = questions
    private val bookmarks = MutableStateFlow(questions.filter { it.isBookmarked }.map { it.id })

    var seedCalls = 0
        private set
    var failWith: Exception? = null
    var lastSearch: String? = null
        private set

    override suspend fun ensureSeeded() {
        failWith?.let { throw it }
        seedCalls++
    }

    override suspend fun getCatalog(): Catalog {
        failWith?.let { throw it }
        return catalog
    }

    override suspend fun getPaperSummaries(subjectId: String): List<PaperSummary> =
        ordered.filter { it.subjectId == subjectId }
            .flatMap { q -> q.appearances.map { PaperKey(subjectId, it.boardId, it.year) to q } }
            .groupBy({ it.first }) { it.second }
            .map { (key, qs) -> PaperSummary(key, qs.count { it.isMcq }, qs.count { !it.isMcq }) }

    override suspend fun getPaperQuestions(key: PaperKey): List<Question> {
        failWith?.let { throw it }
        return ordered.filter { it.subjectId == key.subjectId }
            .mapNotNull { q -> q.appearances.firstOrNull { it.boardId == key.boardId && it.year == key.year }?.let { q to it.number } }
            .sortedBy { it.second }
            .map { it.first.withBookmark() }
    }

    override suspend fun getChapterQuestions(chapterId: String): List<Question> =
        ordered.filter { it.chapterId == chapterId }.sortedByDescending { it.repeatCount }.map { it.withBookmark() }

    override suspend fun getSubjectQuestions(subjectId: String): List<Question> =
        ordered.filter { it.subjectId == subjectId }.map { it.withBookmark() }

    override suspend fun search(query: String, exam: ExamType, limit: Int): List<Question> {
        lastSearch = query
        val subjects = catalog.subjectsFor(exam).map { it.id }.toSet()
        return ordered.filter { it.subjectId in subjects && it.prompt.lowercase().contains(query) }.take(limit).map { it.withBookmark() }
    }

    override suspend fun getQuestion(id: String): Question? = questions[id]?.withBookmark()

    override fun observeBookmarkedQuestions(): Flow<List<Question>> =
        bookmarks.map { ids -> ids.mapNotNull { questions[it]?.copy(isBookmarked = true) } }

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
