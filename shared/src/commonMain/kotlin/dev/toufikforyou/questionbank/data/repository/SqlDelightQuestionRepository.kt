package dev.toufikforyou.questionbank.data.repository

import dev.toufikforyou.questionbank.data.cache.SuspendLazy
import dev.toufikforyou.questionbank.data.db.QuestionBankDatabase
import dev.toufikforyou.questionbank.data.local.encodeOptions
import dev.toufikforyou.questionbank.data.local.toDomain
import dev.toufikforyou.questionbank.data.seed.SeedParser
import dev.toufikforyou.questionbank.data.seed.SeedReader
import dev.toufikforyou.questionbank.domain.model.Category
import dev.toufikforyou.questionbank.domain.model.Question
import dev.toufikforyou.questionbank.domain.model.TimeProvider
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
 * SQLite-backed question repository with a two-level memory cache:
 *  - the catalog (categories + questions) is immutable after seeding, so it is loaded once;
 *  - bookmark ids are mirrored in a StateFlow and updated write-through.
 * Screens therefore get their data synchronously from memory after the first load.
 */
class SqlDelightQuestionRepository(
    private val database: QuestionBankDatabase,
    private val seedReader: SeedReader,
    private val timeProvider: TimeProvider,
    private val ioDispatcher: CoroutineDispatcher,
) : QuestionRepository {

    private val queries get() = database.questionBankQueries
    private val seedMutex = Mutex()
    private val bookmarkMutex = Mutex()
    private var seeded = false

    private val catalog = SuspendLazy { loadCatalog() }
    private val bookmarkIds = MutableStateFlow<List<String>?>(null)

    override suspend fun ensureSeeded() {
        if (seeded) return
        seedMutex.withLock {
            if (seeded) return
            withContext(ioDispatcher) {
                val bundle = SeedParser.parse(seedReader.read())
                val installed = queries.selectSeedVersion().executeAsOneOrNull() ?: 0L
                if (installed < bundle.version) {
                    database.transaction {
                        queries.deleteAllQuestions()
                        queries.deleteAllCategories()
                        bundle.categories.forEachIndexed { index, c ->
                            queries.upsertCategory(c.id, c.title, c.description, index.toLong())
                        }
                        bundle.questions.forEachIndexed { index, q ->
                            queries.upsertQuestion(
                                id = q.id,
                                category_id = q.categoryId,
                                prompt = q.prompt,
                                options_json = encodeOptions(q.options),
                                correct_option_id = q.correctOptionId,
                                explanation = q.explanation,
                                difficulty = q.difficulty.uppercase(),
                                sort_order = index.toLong(),
                            )
                        }
                        queries.deleteOrphanBookmarks()
                        queries.upsertSeedVersion(bundle.version.toLong())
                    }
                    catalog.invalidate()
                    bookmarkIds.value = null
                }
            }
            seeded = true
        }
    }

    override fun observeCategories(): Flow<List<Category>> = flow {
        emit(catalog().categories)
    }

    override suspend fun getQuestions(categoryId: String?): List<Question> {
        val snapshot = catalog()
        val ids = bookmarks().toSet()
        return snapshot.questions
            .filter { categoryId == null || it.categoryId == categoryId }
            .map { if (it.id in ids) it.copy(isBookmarked = true) else it }
    }

    override suspend fun getQuestion(id: String): Question? {
        val question = catalog().byId[id] ?: return null
        return question.copy(isBookmarked = id in bookmarks())
    }

    override fun observeBookmarkedQuestions(): Flow<List<Question>> = flow {
        val snapshot = catalog()
        bookmarks()
        emitAll(
            bookmarkIds.filterNotNull().map { ids ->
                ids.mapNotNull { id -> snapshot.byId[id]?.copy(isBookmarked = true) }
            },
        )
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

    private suspend fun catalog(): Catalog {
        ensureSeeded()
        return catalog.get()
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
        val questions = queries.selectQuestions().executeAsList().map { it.toDomain() }
        val counts = questions.groupingBy { it.categoryId }.eachCount()
        val categories = queries.selectCategories().executeAsList().map {
            Category(
                id = it.id,
                title = it.title,
                description = it.description,
                questionCount = counts[it.id] ?: 0,
            )
        }
        Catalog(categories, questions)
    }

    private class Catalog(val categories: List<Category>, val questions: List<Question>) {
        val byId: Map<String, Question> = questions.associateBy { it.id }
    }
}
