package com.lexumi.app.domain.usecase

import com.lexumi.app.domain.model.AnswerCheck

/**
 * Compares a typed sentence to the expected one word-by-word rather than as
 * one solid string, so a missing final period, an extra space, or different
 * spacing between words never counts as a mistake. Exactly one word may be
 * off by a single letter (same tolerance as the word-learning engine); any
 * more than that, or a genuinely wrong word, counts as an error. The result
 * carries enough detail (which word index differs, and against which
 * original word) for the UI to highlight just that part in red.
 */
object SentenceChecker {

    data class Result(
        val check: AnswerCheck,
        /** The expected sentence split into its original words (for display). */
        val correctWords: List<String>,
        /** Index of the one word that differed, if any. */
        val badWordIndex: Int?,
        /** What the user actually typed at that word position, if any. */
        val userWordAtBadIndex: String?,
    )

    fun check(userInput: String, expected: String): Result {
        val userWords = tokenize(userInput)
        val correctWords = tokenize(expected)

        if (userWords.size != correctWords.size) {
            return Result(AnswerCheck.Wrong(expected), correctWords, null, null)
        }

        var minorIndex = -1
        var wrongIndex = -1

        for (i in correctWords.indices) {
            val isLast = i == correctWords.lastIndex
            val typed = if (isLast) stripTrailingPunctuation(userWords[i]) else userWords[i]
            val correct = if (isLast) stripTrailingPunctuation(correctWords[i]) else correctWords[i]

            if (typed.equals(correct, ignoreCase = true)) continue

            val distance = levenshtein(typed.lowercase(), correct.lowercase())
            if (distance == 1 && minorIndex == -1 && wrongIndex == -1) {
                minorIndex = i
            } else {
                wrongIndex = i
                break
            }
        }

        val check = when {
            wrongIndex != -1 -> AnswerCheck.Wrong(expected)
            minorIndex != -1 -> AnswerCheck.OneLetterTypo(expected)
            else -> AnswerCheck.Correct
        }
        val badIndex = if (wrongIndex != -1) wrongIndex else minorIndex.takeIf { it != -1 }
        return Result(check, correctWords, badIndex, badIndex?.let { userWords.getOrNull(it) })
    }

    private fun tokenize(s: String): List<String> =
        s.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

    private fun stripTrailingPunctuation(word: String): String =
        word.trimEnd('.', '!', '?', ',', ';', ':')

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
