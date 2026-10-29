@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package dev.toufikforyou.questionbank.platform

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.toufikforyou.questionbank.data.db.QuestionBankDatabase
import dev.toufikforyou.questionbank.data.local.DatabaseDriverFactory
import dev.toufikforyou.questionbank.data.seed.SeedReader
import dev.toufikforyou.questionbank.di.AppContainer
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

class IosDatabaseDriverFactory(private val databaseName: String) : DatabaseDriverFactory {
    override fun create(): SqlDriver = NativeSqliteDriver(QuestionBankDatabase.Schema, databaseName)
}

class BundleSeedReader(
    private val resourceName: String = "questions",
    private val resourceType: String = "json",
) : SeedReader {
    override suspend fun read(): String = withContext(Dispatchers.IO) {
        val path = NSBundle.mainBundle.pathForResource(resourceName, resourceType)
            ?: error("Seed resource $resourceName.$resourceType is missing from the app bundle")
        NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)
            ?: error("Seed resource at $path could not be read")
    }
}

/** Swift entry point: `IosAppContainerFactory.shared.create(databaseName: "questionbank.db")`. */
object IosAppContainerFactory {
    const val DEFAULT_DATABASE_NAME = "questionbank.db"

    fun create(databaseName: String): AppContainer = AppContainer(
        driverFactory = IosDatabaseDriverFactory(databaseName),
        seedReader = BundleSeedReader(),
        ioDispatcher = Dispatchers.IO,
        mainDispatcher = Dispatchers.Main.immediate,
    )
}
