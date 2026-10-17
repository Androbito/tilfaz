package com.androbito.tilfaz.core.model

import java.text.Normalizer

/** Text handling for titles as providers write them: language tags, bars, accents, and Arabic letter variants. */
object TitleText {
    // A leading tag such as |FR|, |EN| or "| Sub - ARA |", on a title whose remainder has no further bars.
    private val leadingTag = Regex("""^\|\s*([A-Za-z]{2,4}|Sub\s*-\s*[A-Za-z]{2,3})\s*\|\s*""")
    private const val SEPARATOR = " · "
    private const val SEPARATOR_RUN = 3

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

    /** What the catalog stores to search on: the normalised title, or nothing for a heading entry that must never match. */
    fun searchKey(title: String): String = if (isSeparator(title)) "" else normalize(title)

    /**
     * Providers pad their lists with heading entries such as "-----| BEIN SPORTS |-----" or "=====": they open with a run
     * of the same symbol, or have no letter at all. They are not something to watch, so search leaves them out.
     */
    fun isSeparator(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.none { it.isLetterOrDigit() }) return true
        val first = trimmed.first()
        return !first.isLetterOrDigit() && trimmed.take(SEPARATOR_RUN).let { it.length == SEPARATOR_RUN && it.all { c -> c == first } }
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
