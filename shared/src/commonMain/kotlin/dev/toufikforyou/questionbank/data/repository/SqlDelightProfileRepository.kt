package dev.toufikforyou.questionbank.data.repository

import dev.toufikforyou.questionbank.data.db.QuestionBankDatabase
import dev.toufikforyou.questionbank.domain.model.TimeProvider
import dev.toufikforyou.questionbank.domain.model.UserProfile
import dev.toufikforyou.questionbank.domain.repository.ProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.random.Random

class SqlDelightProfileRepository(
    private val database: QuestionBankDatabase,
    private val timeProvider: TimeProvider,
    private val ioDispatcher: CoroutineDispatcher,
    private val random: Random = Random.Default,
) : ProfileRepository {

    private val queries get() = database.questionBankQueries
    private val mutex = Mutex()
    private var cached: UserProfile? = null

    override suspend fun getOrCreateProfile(): UserProfile = cached ?: mutex.withLock {
        cached ?: withContext(ioDispatcher) {
            database.transactionWithResult {
                queries.selectProfile().executeAsOneOrNull()?.let {
                    UserProfile(it.id, it.display_name, it.created_at)
                } ?: UserProfile(
                    id = newLocalId(),
                    displayName = DEFAULT_DISPLAY_NAME,
                    createdAtMillis = timeProvider.nowMillis(),
                ).also { queries.insertProfile(it.id, it.displayName, it.createdAtMillis) }
            }
        }.also { cached = it }
    }

    private fun newLocalId(): String =
        random.nextBytes(16).joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

    companion object {
        const val DEFAULT_DISPLAY_NAME = "Learner"
    }
}
