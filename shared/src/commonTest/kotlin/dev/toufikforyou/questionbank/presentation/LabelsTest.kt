package dev.toufikforyou.questionbank.presentation

import dev.toufikforyou.questionbank.domain.model.toBanglaDigits
import dev.toufikforyou.questionbank.fakes.at
import dev.toufikforyou.questionbank.fakes.testCatalog
import dev.toufikforyou.questionbank.presentation.format.Labels
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LabelsTest {

    @Test
    fun digitsAndClock() {
        assertEquals("২০২৩", 2023.toBanglaDigits())
        assertEquals("৩.১৪ মি", "3.14 মি".toBanglaDigits())
        assertEquals("১২:০৫", Labels.clock(725))
        assertEquals("০০:০০", Labels.clock(-5))
    }

    @Test
    fun boardTagsAndRepeatLabel() {
        val catalog = testCatalog()

        assertEquals(listOf("ঢাকা বোর্ড ২০২৩"), Labels.appearanceTags(catalog, listOf(at("dhaka", 2023, 1))))
        assertNull(Labels.repeat(1))
        assertEquals("২ বার এসেছে", Labels.repeat(2))
        assertEquals("প্রশ্ন ৩ / ১০", Labels.progress(3, 10))
    }
}
