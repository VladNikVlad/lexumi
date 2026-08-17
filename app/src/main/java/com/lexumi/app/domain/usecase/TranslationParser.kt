package com.lexumi.app.domain.usecase

/**
 * A word's `translation` (or `term`) field can contain more than one
 * acceptable variant separated by "/", e.g. "більш-менш / так собі", and any
 * variant may carry a short "(...)" explanation of what it means, e.g.
 * "так собі (коли щось середнє)". The explanation is shown to the user but
 * must never be required as part of a typed/chosen answer.
 */
object TranslationParser {

    /** All variants that should be accepted as correct, explanation text stripped and trimmed. */
    fun acceptableAnswers(raw: String): List<String> {
        val variants = raw.split("/")
            .map { trimPunctuation(stripExplanation(it).trim()) }
            .filter { it.isNotBlank() }
        return variants.ifEmpty { listOf(raw.trim()) }
    }

    /** The first variant, explanation stripped — a clean single line for buttons, tiles, and TTS. */
    fun displayPrimary(raw: String): String =
        acceptableAnswers(raw).firstOrNull() ?: raw.trim()

    /** True when there's more to show than [displayPrimary] alone (extra variants and/or an explanation). */
    fun hasExtra(raw: String): Boolean = raw.trim() != displayPrimary(raw)

    private fun stripExplanation(text: String): String =
        text.replace(Regex("\\([^)]*\\)"), "")

    /** Drops trailing punctuation a variant might end with (e.g. a "/"-split piece that kept a
     * stray period or comma from the original sentence), so it doesn't get compared/shown with it. */
    private fun trimPunctuation(word: String): String =
        word.trim { it in ".,!?" }
}
