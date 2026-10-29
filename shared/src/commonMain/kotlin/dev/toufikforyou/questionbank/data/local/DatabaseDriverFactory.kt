package dev.toufikforyou.questionbank.data.local

import app.cash.sqldelight.db.SqlDriver

/** Platform-specific SQLite driver creation (Android / iOS native). */
fun interface DatabaseDriverFactory {
    fun create(): SqlDriver
}
