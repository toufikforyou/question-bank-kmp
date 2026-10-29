package dev.toufikforyou.questionbank.data.seed

import kotlinx.serialization.json.Json

/** Reads the raw JSON seed shipped with the app (Android assets / iOS bundle). */
fun interface SeedReader {
    suspend fun read(): String
}

class SeedValidationException(message: String) : IllegalStateException(message)

object SeedParser {
    internal val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun parse(raw: String): SeedBundle {
        val bundle = json.decodeFromString(SeedBundle.serializer(), raw)
        validate(bundle)
        return bundle
    }

    private fun validate(bundle: SeedBundle) {
        fun fail(message: String): Nothing = throw SeedValidationException(message)

        if (bundle.version < 1) fail("Seed version must be >= 1")
        val categoryIds = bundle.categories.map { it.id }
        if (categoryIds.size != categoryIds.toSet().size) fail("Duplicate category ids in seed")
        val questionIds = bundle.questions.map { it.id }
        if (questionIds.size != questionIds.toSet().size) fail("Duplicate question ids in seed")

        val known = categoryIds.toSet()
        bundle.questions.forEach { q ->
            if (q.categoryId !in known) fail("Question ${q.id} references unknown category ${q.categoryId}")
            if (q.options.size < 2) fail("Question ${q.id} needs at least two options")
            if (q.options.none { it.id == q.correctOptionId }) fail("Question ${q.id} has no correct option")
        }
    }
}
