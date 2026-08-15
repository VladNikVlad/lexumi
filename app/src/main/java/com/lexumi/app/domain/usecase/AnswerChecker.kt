package com.lexumi.app.domain.usecase

import com.lexumi.app.domain.model.AnswerCheck

/**
 * Compares what the user typed against the expected spelling.
 * [expected] may itself list several acceptable variants separated by "/"
 * (each possibly carrying a "(...)" explanation that must not be required as
 * part of the answer) — any of them counts as correct.
 * A one-letter mistake (wrong letter, one missing, or one extra) earns half
 * credit and highlights just that letter; anything further off is a full miss
 * and the correct spelling is shown (point 20 & 21 of the scenario).
 */
object AnswerChecker {

    fun check(userInput: String, expected: String): AnswerCheck {
        val a = userInput.trim()
        val variants = TranslationParser.acceptableAnswers(expected)
        if (variants.any { it.equals(a, ignoreCase = true) }) return AnswerCheck.Correct

        val closest = variants.minByOrNull { levenshtein(a.lowercase(), it.lowercase()) } ?: expected.trim()
        val distance = levenshtein(a.lowercase(), closest.lowercase())
        // Shown to the user as "the correct answer" — just the first/primary variant, not the
        // raw "варіант1 / варіант2 (пояснення)" field value.
        val display = TranslationParser.displayPrimary(expected)
        return if (distance == 1) {
            AnswerCheck.OneLetterTypo(correctSpelling = display)
        } else {
            AnswerCheck.Wrong(correctSpelling = display)
        }
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        return dp[a.length][b.length]
    }
}
