package dev.toufikforyou.questionbank.data.seed

import kotlinx.serialization.Serializable

@Serializable
data class SeedBundle(
    val version: Int,
    val sample: Boolean = false,
    val exams: List<SeedExam>,
    val boards: List<SeedBoard>,
    val notes: List<SeedNote> = emptyList(),
    val subjects: List<SeedSubject>,
    val questions: List<SeedQuestion>,
)

@Serializable
data class SeedExam(val id: String, val name: String, val fullName: String)

@Serializable
data class SeedBoard(val id: String, val name: String, val nameEn: String)

@Serializable
data class SeedNote(val exam: String, val year: Int, val text: String)

@Serializable
data class SeedSubject(
    val id: String,
    val exam: String,
    val name: String,
    val nameEn: String,
    val code: String,
    val group: String,
    val chapters: List<SeedChapter>,
)

@Serializable
data class SeedChapter(val id: String, val number: Int, val name: String)

@Serializable
data class SeedQuestion(
    val id: String,
    val subjectId: String,
    val chapterId: String,
    val type: String,
    val prompt: String? = null,
    val options: List<OptionDto> = emptyList(),
    val correctOptionId: String? = null,
    val explanation: String? = null,
    val stem: String? = null,
    val parts: List<PartDto> = emptyList(),
    /** "boardId:year:number" entries. */
    val appearances: List<String> = emptyList(),
)

@Serializable
data class OptionDto(val id: String, val text: String)

@Serializable
data class PartDto(val label: String, val text: String, val marks: Int, val answer: String)

/** A decoded "boardId:year:number" appearance. */
data class SeedAppearance(val boardId: String, val year: Int, val number: Int) {
    companion object {
        fun parse(raw: String): SeedAppearance? {
            val parts = raw.split(':')
            if (parts.size != 3) return null
            val year = parts[1].toIntOrNull() ?: return null
            val number = parts[2].toIntOrNull() ?: return null
            return SeedAppearance(parts[0], year, number)
        }
    }
}
