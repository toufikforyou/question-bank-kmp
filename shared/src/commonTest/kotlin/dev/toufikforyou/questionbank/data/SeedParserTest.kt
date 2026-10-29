package dev.toufikforyou.questionbank.data

import dev.toufikforyou.questionbank.data.seed.SeedAppearance
import dev.toufikforyou.questionbank.data.seed.SeedParser
import dev.toufikforyou.questionbank.data.seed.SeedValidationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SeedParserTest {

    private fun seed(correct: String = "ক", board: String = "dhaka", type: String = "MCQ") = """
        {"version": 4, "sample": true,
         "exams": [{"id": "SSC", "name": "এসএসসি", "fullName": "মাধ্যমিক"}],
         "boards": [{"id": "dhaka", "name": "ঢাকা", "nameEn": "Dhaka", "extra": 1}],
         "subjects": [{"id": "s", "exam": "SSC", "name": "পদার্থ", "nameEn": "Physics", "code": "136", "group": "SCIENCE",
                       "chapters": [{"id": "s-1", "number": 1, "name": "অধ্যায়"}]}],
         "questions": [{"id": "q1", "subjectId": "s", "chapterId": "s-1", "type": "$type", "prompt": "?",
                        "options": [{"id": "ক", "text": "A"}, {"id": "খ", "text": "B"}], "correctOptionId": "$correct",
                        "appearances": ["$board:2023:1"]}]}
    """.trimIndent()

    @Test
    fun parsesValidSeedAndPeeksVersionCheaply() {
        val bundle = SeedParser.parse(seed())

        assertEquals(4, bundle.version)
        assertEquals(4, SeedParser.peekVersion(seed()))
        assertEquals(SeedAppearance("dhaka", 2023, 1), SeedAppearance.parse(bundle.questions.single().appearances.single()))
    }

    @Test
    fun rejectsMissingCorrectOptionUnknownBoardAndCreativeWithoutParts() {
        assertFailsWith<SeedValidationException> { SeedParser.parse(seed(correct = "z")) }
        assertFailsWith<SeedValidationException> { SeedParser.parse(seed(board = "nowhere")) }
        assertFailsWith<SeedValidationException> { SeedParser.parse(seed(type = "CQ")) }
    }

    @Test
    fun malformedAppearanceIsNull() {
        assertNull(SeedAppearance.parse("dhaka-2023"))
        assertNull(SeedAppearance.parse("dhaka:year:1"))
    }
}
