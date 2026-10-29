package dev.toufikforyou.questionbank.platform

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.toufikforyou.questionbank.data.db.QuestionBankDatabase
import dev.toufikforyou.questionbank.data.local.DatabaseDriverFactory
import dev.toufikforyou.questionbank.data.seed.SeedReader
import dev.toufikforyou.questionbank.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** @param databaseName `null` creates an in-memory database (used by instrumentation tests). */
class AndroidDatabaseDriverFactory(
    private val context: Context,
    private val databaseName: String? = DEFAULT_DATABASE_NAME,
) : DatabaseDriverFactory {
    override fun create(): SqlDriver =
        AndroidSqliteDriver(QuestionBankDatabase.Schema, context.applicationContext, databaseName)

    companion object {
        const val DEFAULT_DATABASE_NAME = "questionbank.db"
    }
}

class AssetSeedReader(
    private val context: Context,
    private val assetName: String = DEFAULT_SEED_ASSET,
) : SeedReader {
    override suspend fun read(): String = withContext(Dispatchers.IO) {
        context.assets.open(assetName).bufferedReader().use { it.readText() }
    }

    companion object {
        const val DEFAULT_SEED_ASSET = "questions.json"
    }
}

fun createAndroidAppContainer(
    context: Context,
    databaseName: String? = AndroidDatabaseDriverFactory.DEFAULT_DATABASE_NAME,
): AppContainer = AppContainer(
    driverFactory = AndroidDatabaseDriverFactory(context, databaseName),
    seedReader = AssetSeedReader(context),
    ioDispatcher = Dispatchers.IO,
    mainDispatcher = Dispatchers.Main.immediate,
)
