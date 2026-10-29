package dev.toufikforyou.questionbank.data.repository

import dev.toufikforyou.questionbank.data.cache.SuspendLazy
import dev.toufikforyou.questionbank.data.db.QuestionBankDatabase
import dev.toufikforyou.questionbank.data.local.encodeOptions
import dev.toufikforyou.questionbank.data.local.encodeParts
import dev.toufikforyou.questionbank.data.local.searchText
import dev.toufikforyou.questionbank.data.local.toDomain
import dev.toufikforyou.questionbank.data.seed.SeedAppearance
import dev.toufikforyou.questionbank.data.seed.SeedBundle
import dev.toufikforyou.questionbank.data.seed.SeedParser
import dev.toufikforyou.questionbank.data.seed.SeedReader
import dev.toufikforyou.questionbank.domain.model.Appearance
import dev.toufikforyou.questionbank.domain.model.Board
import dev.toufikforyou.questionbank.domain.model.Catalog
import dev.toufikforyou.questionbank.domain.model.Chapter
import dev.toufikforyou.questionbank.domain.model.Exam
import dev.toufikforyou.questionbank.domain.model.ExamType
import dev.toufikforyou.questionbank.domain.model.PaperKey
import dev.toufikforyou.questionbank.domain.model.PaperSummary
import dev.toufikforyou.questionbank.domain.model.Question
import dev.toufikforyou.questionbank.domain.model.QuestionType
import dev.toufikforyou.questionbank.domain.model.StudyGroup
import dev.toufikforyou.questionbank.domain.model.Subject
import dev.toufikforyou.questionbank.domain.model.TimeProvider
import dev.toufikforyou.questionbank.domain.model.YearNote
import dev.toufikforyou.questionbank.domain.repository.QuestionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * SQLite-backed question bank with an in-memory cache:
 *  - the catalog (exams, boards, subjects, chapters) is loaded once;
 *  - questions are cached by id the first time any screen loads them, together with the
 *    id lists of papers and chapters, so revisiting a screen never touches the database;
 *  - bookmark ids are mirrored in a StateFlow and written through.
 * The bank is immutable after seeding, which keeps the cache trivially consistent.
 */
class SqlDelightQuestionRepository(
    private val database: QuestionBankDatabase,
    private val seedReader: SeedReader,
    private val timeProvider: TimeProvider,
    private val ioDispatcher: CoroutineDispatcher,
) : QuestionRepository {

    private val queries get() = database.questionBankQueries
    private val seedMutex = Mutex()
    private val cacheMutex = Mutex()
    private val bookmarkMutex = Mutex()
    private var seeded = false

    private val catalog = SuspendLazy { loadCatalog() }
    private val questionsById = HashMap<String, Question>()
    private val paperIds = HashMap<PaperKey, List<String>>()
    private val chapterIds = HashMap<String, List<String>>()
    private val subjectIds = HashMap<String, List<String>>()
    private val summaries = HashMap<String, List<PaperSummary>>()
    private val bookmarkIds = MutableStateFlow<List<String>?>(null)

    override suspend fun ensureSeeded() {
        if (seeded) return
        seedMutex.withLock {
            if (seeded) return
            withContext(ioDispatcher) {
                val installed = queries.selectSeedMeta().executeAsOneOrNull()?.version ?: 0L
                val raw = seedReader.read()
                val available = SeedParser.peekVersion(raw)
                if (available == null || installed < available) {
                    import(SeedParser.parse(raw))
                    catalog.invalidate()
                    cacheMutex.withLock {
                        questionsById.clear(); paperIds.clear(); chapterIds.clear(); subjectIds.clear(); summaries.clear()
                    }
                    bookmarkIds.value = null
                }
            }
            seeded = true
        }
    }

    private fun import(bundle: SeedBundle) = database.transaction {
        queries.clearAppearances()
        queries.clearQuestions()
        queries.clearChapters()
        queries.clearSubjects()
        queries.clearNotes()
        queries.clearBoards()
        queries.clearExams()
        bundle.exams.forEachIndexed { i, e -> queries.insertExam(e.id, e.name, e.fullName, i.toLong()) }
        bundle.boards.forEachIndexed { i, b -> queries.insertBoard(b.id, b.name, b.nameEn, i.toLong()) }
        bundle.notes.forEach { queries.insertNote(it.exam, it.year.toLong(), it.text) }
        bundle.subjects.forEachIndexed { i, s ->
            queries.insertSubject(s.id, s.exam, s.name, s.nameEn, s.code, s.group, i.toLong())
            s.chapters.forEach { c -> queries.insertChapter(c.id, s.id, c.number.toLong(), c.name) }
        }
        bundle.questions.forEach { q ->
            queries.insertQuestion(
                id = q.id,
                subject_id = q.subjectId,
                chapter_id = q.chapterId,
                type = q.type,
                prompt = q.prompt ?: q.stem.orEmpty(),
                options_json = encodeOptions(q.options),
                correct_option_id = q.correctOptionId,
                explanation = q.explanation,
                parts_json = encodeParts(q.parts),
                search_text = q.searchText(),
                appearance_count = q.appearances.size.toLong(),
            )
            q.appearances.mapNotNull(SeedAppearance::parse).forEach { a ->
                queries.insertAppearance(q.id, q.subjectId, a.boardId, a.year.toLong(), a.number.toLong())
            }
        }
        queries.deleteOrphanBookmarks()
        queries.upsertSeedMeta(bundle.version.toLong(), if (bundle.sample) 1L else 0L)
    }

    override suspend fun getCatalog(): Catalog {
        ensureSeeded()
        return catalog.get()
    }

    override suspend fun getPaperSummaries(subjectId: String): List<PaperSummary> {
        ensureSeeded()
        cacheMutex.withLock { summaries[subjectId] }?.let { return it }
        val loaded = withContext(ioDispatcher) {
            queries.selectPaperSummaries(subjectId).executeAsList()
                .groupBy { PaperKey(subjectId, it.board_id, it.year.toInt()) }
                .map { (key, rows) ->
                    PaperSummary(
                        key = key,
                        mcqCount = rows.filter { it.type == QuestionType.MCQ.name }.sumOf { it.total }.toInt(),
                        cqCount = rows.filter { it.type == QuestionType.CQ.name }.sumOf { it.total }.toInt(),
                    )
                }
        }
        cacheMutex.withLock { summaries[subjectId] = loaded }
        return loaded
    }

    override suspend fun getPaperQuestions(key: PaperKey): List<Question> {
        ensureSeeded()
        val ids = cacheMutex.withLock { paperIds[key] } ?: withContext(ioDispatcher) {
            queries.selectPaperQuestionIds(key.subjectId, key.boardId, key.year.toLong()).executeAsList().map { it.question_id }
        }.also { ids -> cacheMutex.withLock { paperIds[key] = ids } }
        return questions(ids)
    }

    override suspend fun getChapterQuestions(chapterId: String): List<Question> {
        ensureSeeded()
        val ids = cacheMutex.withLock { chapterIds[chapterId] } ?: withContext(ioDispatcher) {
            queries.selectChapterQuestions(chapterId).executeAsList().map { it.id }
        }.also { ids -> cacheMutex.withLock { chapterIds[chapterId] = ids } }
        return questions(ids)
    }

    override suspend fun getSubjectQuestions(subjectId: String): List<Question> {
        ensureSeeded()
        val ids = cacheMutex.withLock { subjectIds[subjectId] } ?: withContext(ioDispatcher) {
            queries.selectSubjectQuestions(subjectId).executeAsList().map { it.id }
        }.also { ids -> cacheMutex.withLock { subjectIds[subjectId] = ids } }
        return questions(ids)
    }

    override suspend fun search(query: String, exam: ExamType, limit: Int): List<Question> {
        ensureSeeded()
        val ids = withContext(ioDispatcher) {
            queries.searchQuestions(exam.name, query, limit.toLong()).executeAsList().map { it.id }
        }
        return questions(ids)
    }

    override suspend fun getQuestion(id: String): Question? = questions(listOf(id)).firstOrNull()

    override fun observeBookmarkedQuestions(): Flow<List<Question>> = flow {
        bookmarks()
        emitAll(bookmarkIds.filterNotNull().map { ids -> questions(ids) })
    }

    override suspend fun setBookmarked(questionId: String, bookmarked: Boolean) {
        bookmarks()
        bookmarkMutex.withLock {
            withContext(ioDispatcher) {
                if (bookmarked) {
                    queries.insertBookmark(questionId, timeProvider.nowMillis())
                } else {
                    queries.deleteBookmark(questionId)
                }
            }
            bookmarkIds.update { current ->
                val without = current.orEmpty().filterNot { it == questionId }
                if (bookmarked) listOf(questionId) + without else without
            }
        }
    }

    /** Resolves ids to questions in the given order, loading only what is not cached yet. */
    private suspend fun questions(ids: List<String>): List<Question> {
        if (ids.isEmpty()) return emptyList()
        ensureSeeded()
        val missing = cacheMutex.withLock { ids.filterNot { it in questionsById } }.distinct()
        if (missing.isNotEmpty()) {
            val loaded = withContext(ioDispatcher) {
                missing.chunked(SQL_CHUNK).flatMap { chunk ->
                    val appearances = queries.selectAppearancesByQuestionIds(chunk).executeAsList()
                        .groupBy({ it.question_id }) { Appearance(it.board_id, it.year.toInt(), it.number.toInt()) }
                    queries.selectQuestionsByIds(chunk).executeAsList().map { it.toDomain(appearances[it.id].orEmpty()) }
                }
            }
            cacheMutex.withLock { loaded.forEach { questionsById[it.id] = it } }
        }
        val marked = bookmarks().toSet()
        return cacheMutex.withLock {
            ids.mapNotNull { id -> questionsById[id]?.let { if (id in marked) it.copy(isBookmarked = true) else it } }
        }
    }

    private suspend fun bookmarks(): List<String> {
        bookmarkIds.value?.let { return it }
        return bookmarkMutex.withLock {
            bookmarkIds.value ?: withContext(ioDispatcher) {
                queries.selectBookmarkIds().executeAsList()
            }.also { bookmarkIds.value = it }
        }
    }

    private suspend fun loadCatalog(): Catalog = withContext(ioDispatcher) {
        val counts = queries.selectChapterCounts().executeAsList().groupBy { it.chapter_id }
        val chapters = queries.selectChapters().executeAsList().groupBy { it.subject_id }
        val subjects = queries.selectSubjects().executeAsList().mapNotNull { s ->
            val exam = ExamType.entries.firstOrNull { it.name == s.exam } ?: return@mapNotNull null
            Subject(
                id = s.id,
                exam = exam,
                name = s.name,
                nameEn = s.name_en,
                code = s.code,
                group = StudyGroup.entries.firstOrNull { it.name == s.study_group } ?: StudyGroup.COMMON,
                chapters = chapters[s.id].orEmpty().map { c ->
                    val byType = counts[c.id].orEmpty()
                    Chapter(
                        id = c.id,
                        subjectId = s.id,
                        number = c.number.toInt(),
                        name = c.name,
                        mcqCount = byType.filter { it.type == QuestionType.MCQ.name }.sumOf { it.total }.toInt(),
                        cqCount = byType.filter { it.type == QuestionType.CQ.name }.sumOf { it.total }.toInt(),
                    )
                },
            )
        }
        val exams = queries.selectExams().executeAsList().mapNotNull { e ->
            ExamType.entries.firstOrNull { it.name == e.id }?.let { Exam(it, e.name, e.full_name) }
        }
        val years = queries.selectExamYears().executeAsList()
            .mapNotNull { row -> ExamType.entries.firstOrNull { it.name == row.exam }?.let { it to row.year.toInt() } }
            .groupBy({ it.first }) { it.second }
        Catalog(
            exams = exams,
            boards = queries.selectBoards().executeAsList().map { Board(it.id, it.name, it.name_en) },
            subjects = subjects,
            notes = queries.selectNotes().executeAsList().mapNotNull { n ->
                ExamType.entries.firstOrNull { it.name == n.exam }?.let { YearNote(it, n.year.toInt(), n.text) }
            },
            yearsByExam = years,
            isSample = queries.selectSeedMeta().executeAsOneOrNull()?.is_sample == 1L,
        )
    }

    private companion object {
        /** Stays well below SQLite's default limit of 999 bound parameters. */
        const val SQL_CHUNK = 400
    }
}
