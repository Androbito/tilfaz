package com.androbito.tilfaz.core.domain.search

import java.text.Normalizer

/** Text handling for titles as providers write them: language tags, bars, accents, and Arabic letter variants. */
object TitleText {
    // A leading tag such as |FR|, |EN| or "| Sub - ARA |", on a title whose remainder has no further bars.
    private val leadingTag = Regex("""^\|\s*([A-Za-z]{2,4}|Sub\s*-\s*[A-Za-z]{2,3})\s*\|\s*""")
    private const val SEPARATOR = " · "

    /** The text to compare against: lower case, no accents, Arabic variants folded, bars and punctuation as single spaces. */
    fun normalize(text: String): String {
        val withoutTag = withoutTag(text)
        val decomposed = Normalizer.normalize(withoutTag, Normalizer.Form.NFD)
        val out = StringBuilder(decomposed.length)
        var lastWasSpace = true
        for (ch in decomposed) {
            val folded = fold(ch)
            when {
                folded == null -> Unit // a combining accent or Arabic mark
                folded.isLetterOrDigit() -> {
                    out.append(folded)
                    lastWasSpace = false
                }
                !lastWasSpace -> {
                    out.append(' ')
                    lastWasSpace = true
                }
            }
        }
        return out.toString().trimEnd()
    }

    /** The title as the user should read it: no language tag, and a title written between bars joined with dots. */
    fun clean(text: String): String {
        val trimmed = withoutTag(text).trim()
        if (!trimmed.contains('|')) return trimmed
        return trimmed.split('|').map { it.trim() }.filter { it.isNotEmpty() }.joinToString(SEPARATOR)
    }

    private fun withoutTag(text: String): String {
        val match = leadingTag.find(text) ?: return text
        val rest = text.substring(match.range.last + 1)
        return if (rest.contains('|')) text else rest
    }

    /** Lower case with Arabic variants folded; null for a mark that carries no letter of its own. */
    private fun fold(ch: Char): Char? = when {
        Character.getType(ch) == Character.NON_SPACING_MARK.toInt() -> null
        ch == 'ـ' -> null // tatweel
        ch in "أإآٱ" -> 'ا'
        ch == 'ى' -> 'ي'
        ch == 'ة' -> 'ه'
        ch == 'ؤ' -> 'و'
        ch == 'ئ' -> 'ي'
        else -> ch.lowercaseChar()
    }
}
