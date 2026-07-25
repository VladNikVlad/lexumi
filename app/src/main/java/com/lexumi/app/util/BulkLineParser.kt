package com.lexumi.app.util

/**
 * Parses lines like "apple - яблуко" or "I like apples - Мені подобаються
 * яблука / Я люблю яблука" into (main, translation, extraTranslations).
 * Accepts "-", "=", "–" or a tab as the term/translation separator, and
 * "/" to list several valid translations for sentences.
 */
object BulkLineParser {

    data class ParsedLine(val main: String, val translation: String, val extraTranslations: List<String> = emptyList())

    private val separators = listOf(" - ", " – ", " = ", "\t")

    fun parse(text: String): List<ParsedLine> =
        text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { splitLine(it) }

    private fun splitLine(line: String): ParsedLine? {
        for (sep in separators) {
            val idx = line.indexOf(sep)
            if (idx > 0) {
                val main = line.substring(0, idx).trim()
                val rest = line.substring(idx + sep.length).trim()
                if (main.isEmpty() || rest.isEmpty()) continue
                val translations = rest.split("/").map { it.trim() }.filter { it.isNotBlank() }
                if (translations.isEmpty()) continue
                return ParsedLine(main, translations.first(), translations.drop(1))
            }
        }
        return null
    }
}
