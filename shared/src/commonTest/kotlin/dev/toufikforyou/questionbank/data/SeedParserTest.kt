package dev.toufikforyou.questionbank.data

import dev.toufikforyou.questionbank.data.seed.SeedParser
import dev.toufikforyou.questionbank.data.seed.SeedValidationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SeedParserTest {

    private fun seed(correct: String = "a", category: String = "c1") = """
        {
          "version": 2,
          "categories": [{ "id": "c1", "title": "Cat", "description": "d", "unknownField": true }],
          "questions": [{
            "id": "q1", "categoryId": "$category", "prompt": "?",
            "options": [{ "id": "a", "text": "A" }, { "id": "b", "text": "B" }],
            "correctOptionId": "$correct"
          }]
        }
    """.trimIndent()

    @Test
    fun parsesValidSeedAndIgnoresUnknownKeys() {
        val bundle = SeedParser.parse(seed())

        assertEquals(2, bundle.version)
        assertEquals("MEDIUM", bundle.questions.single().difficulty)
    }

    @Test
    fun rejectsMissingCorrectOption() {
        assertFailsWith<SeedValidationException> { SeedParser.parse(seed(correct = "z")) }
    }

    @Test
    fun rejectsUnknownCategory() {
        assertFailsWith<SeedValidationException> { SeedParser.parse(seed(category = "nope")) }
    }
}
