package dev.toufikforyou.questionbank.presentation.format

import dev.toufikforyou.questionbank.domain.model.Appearance
import dev.toufikforyou.questionbank.domain.model.Catalog
import dev.toufikforyou.questionbank.domain.model.ExamType
import dev.toufikforyou.questionbank.domain.model.StudyGroup
import dev.toufikforyou.questionbank.domain.model.toBanglaDigits

/** Bangla display strings shared by both apps so they always read the same. */
object Labels {
    const val MAX_TAGS = 4

    fun number(value: Int): String = value.toBanglaDigits()

    /** Bangla digits inside any text, e.g. a subject code "136" -> "১৩৬". */
    fun digits(text: String): String = text.toBanglaDigits()

    fun exam(type: ExamType): String = when (type) {
        ExamType.SSC -> "এসএসসি"
        ExamType.HSC -> "এইচএসসি"
    }

    fun group(group: StudyGroup): String = when (group) {
        StudyGroup.COMMON -> "আবশ্যিক বিষয়"
        StudyGroup.SCIENCE -> "বিজ্ঞান"
        StudyGroup.HUMANITIES -> "মানবিক"
        StudyGroup.BUSINESS -> "ব্যবসায় শিক্ষা"
    }

    fun boardYear(boardName: String, year: Int): String = "$boardName বোর্ড ${year.toBanglaDigits()}"

    fun appearance(catalog: Catalog, appearance: Appearance): String =
        boardYear(catalog.board(appearance.boardId)?.name ?: appearance.boardId, appearance.year)

    /** Up to [MAX_TAGS] "ঢাকা বোর্ড ২০২৩" tags, then "+৩ টি". */
    fun appearanceTags(catalog: Catalog, appearances: List<Appearance>): List<String> {
        val tags = appearances.take(MAX_TAGS).map { appearance(catalog, it) }
        val rest = appearances.size - MAX_TAGS
        return if (rest > 0) tags + "+${rest.toBanglaDigits()}টি" else tags
    }

    fun repeat(count: Int): String? = if (count > 1) "${count.toBanglaDigits()} বার এসেছে" else null

    fun questionCount(mcq: Int, cq: Int): String = "বহুনির্বাচনি ${mcq.toBanglaDigits()} · সৃজনশীল ${cq.toBanglaDigits()}"

    fun chapter(number: Int, name: String): String = "অধ্যায় ${number.toBanglaDigits()}: $name"

    fun marks(marks: Int): String = "${marks.toBanglaDigits()} নম্বর"

    /** 754 -> "১২:৩৪". */
    fun clock(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val minutes = (safe / 60).toString().padStart(2, '0')
        val seconds = (safe % 60).toString().padStart(2, '0')
        return "$minutes:$seconds".toBanglaDigits()
    }

    fun progress(current: Int, total: Int): String = "প্রশ্ন ${current.toBanglaDigits()} / ${total.toBanglaDigits()}"

    fun percent(value: Int): String = "${value.toBanglaDigits()}%"
}
