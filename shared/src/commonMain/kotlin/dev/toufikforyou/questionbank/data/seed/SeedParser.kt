package dev.toufikforyou.questionbank.data.seed

import dev.toufikforyou.questionbank.domain.model.ExamType
import dev.toufikforyou.questionbank.domain.model.QuestionType
import dev.toufikforyou.questionbank.domain.model.StudyGroup
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

    private val versionPattern = Regex("\"version\"\\s*:\\s*(\\d+)")

    /** Reads only the top-level version so app start does not parse the whole bank. */
    fun peekVersion(raw: String): Int? =
        versionPattern.find(raw.take(VERSION_PEEK_CHARS))?.groupValues?.get(1)?.toIntOrNull()

    private const val VERSION_PEEK_CHARS = 256

    fun parse(raw: String): SeedBundle {
        val bundle = json.decodeFromString(SeedBundle.serializer(), raw)
        validate(bundle)
        return bundle
    }

    private fun validate(bundle: SeedBundle) {
        fun fail(message: String): Nothing = throw SeedValidationException(message)

        if (bundle.version < 1) fail("Seed version must be >= 1")
        bundle.exams.forEach { if (ExamType.entries.none { e -> e.name == it.id }) fail("Unknown exam ${it.id}") }
        if (bundle.boards.map { it.id }.toSet().size != bundle.boards.size) fail("Duplicate board ids in seed")

        val subjectIds = bundle.subjects.map { it.id }
        if (subjectIds.size != subjectIds.toSet().size) fail("Duplicate subject ids in seed")
        val chapterIds = bundle.subjects.flatMap { s -> s.chapters.map { it.id } }.toSet()
        bundle.subjects.forEach { s ->
            if (ExamType.entries.none { it.name == s.exam }) fail("Subject ${s.id} has unknown exam ${s.exam}")
            if (StudyGroup.entries.none { it.name == s.group }) fail("Subject ${s.id} has unknown group ${s.group}")
        }

        val questionIds = bundle.questions.map { it.id }
        if (questionIds.size != questionIds.toSet().size) fail("Duplicate question ids in seed")
        val boardIds = bundle.boards.map { it.id }.toSet()
        val subjects = subjectIds.toSet()

        bundle.questions.forEach { q ->
            if (q.subjectId !in subjects) fail("Question ${q.id} references unknown subject ${q.subjectId}")
            if (q.chapterId !in chapterIds) fail("Question ${q.id} references unknown chapter ${q.chapterId}")
            when (q.type) {
                QuestionType.MCQ.name -> {
                    if (q.prompt.isNullOrBlank()) fail("MCQ ${q.id} has no prompt")
                    if (q.options.size < 2) fail("MCQ ${q.id} needs at least two options")
                    if (q.options.none { it.id == q.correctOptionId }) fail("MCQ ${q.id} has no correct option")
                }
                QuestionType.CQ.name -> {
                    if (q.stem.isNullOrBlank()) fail("CQ ${q.id} has no stem")
                    if (q.parts.isEmpty()) fail("CQ ${q.id} has no parts")
                }
                else -> fail("Question ${q.id} has unknown type ${q.type}")
            }
            q.appearances.forEach { raw ->
                val appearance = SeedAppearance.parse(raw) ?: fail("Question ${q.id} has malformed appearance '$raw'")
                if (appearance.boardId !in boardIds) fail("Question ${q.id} references unknown board ${appearance.boardId}")
            }
        }
    }
}
