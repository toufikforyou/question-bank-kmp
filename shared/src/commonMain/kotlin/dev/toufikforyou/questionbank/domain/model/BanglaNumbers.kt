package dev.toufikforyou.questionbank.domain.model

private const val BANGLA_DIGITS = "০১২৩৪৫৬৭৮৯"

/** Renders ASCII digits as Bangla digits, e.g. 2023 -> ২০২৩. */
fun String.toBanglaDigits(): String = buildString(length) {
    for (c in this@toBanglaDigits) append(if (c in '0'..'9') BANGLA_DIGITS[c - '0'] else c)
}

fun Int.toBanglaDigits(): String = toString().toBanglaDigits()
