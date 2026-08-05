package com.lexumi.app.domain.usecase

import com.lexumi.app.domain.model.AnswerCheck

/**
 * Compares a typed sentence to the expected one word-by-word rather than as
 * one solid string, so a missing final period, an extra space, or different
 * spacing between words never counts as a mistake. Reports every word that
 * differs (not just the first), so the caller can decide between "fully
 * correct", "a couple of words wrong — offer a fill-in-the-blanks hint", or
 * "too many mistakes — just reveal the answer".
 */
object SentenceChecker {

    enum class Category { CORRECT, PARTIAL, WRONG }

    data class Result(
        val category: Category,
        val check: AnswerCheck,
        /** The expected sentence split into its original words (for display). */
        val correctWords: List<String>,
        /** Indices (into [correctWords]) of every word that didn't match — meaningful only when word counts line up. */
        val mismatchedIndices: List<Int>,
        /** What the user actually typed at each mismatched index, aligned with [mismatchedIndices]. */
        val userWordsAtMismatches: List<String>,
    )

    /** At most this many wrong words still gets the fill-in-the-blanks hint treatment; more than this is a flat reveal. */
    private const val MAX_PARTIAL_MISTAKES = 2

    fun check(userInput: String, expected: String): Result {
        val userWords = tokenize(userInput)
        val correctWords = tokenize(expected)

        if (userWords.size != correctWords.size) {
            return Result(Category.WRONG, AnswerCheck.Wrong(expected), correctWords, emptyList(), emptyList())
        }

        val mismatches = mutableListOf<Int>()
        for (i in correctWords.indices) {
            val isLast = i == correctWords.lastIndex
            val typed = if (isLast) stripTrailingPunctuation(userWords[i]) else userWords[i]
            val correct = if (isLast) stripTrailingPunctuation(correctWords[i]) else correctWords[i]
            if (!typed.equals(correct, ignoreCase = true)) mismatches.add(i)
        }

        val category = when {
            mismatches.isEmpty() -> Category.CORRECT
            mismatches.size <= MAX_PARTIAL_MISTAKES -> Category.PARTIAL
            else -> Category.WRONG
        }
        val check = when (category) {
            Category.CORRECT -> AnswerCheck.Correct
            Category.PARTIAL -> AnswerCheck.OneLetterTypo(expected)
            Category.WRONG -> AnswerCheck.Wrong(expected)
        }
        return Result(category, check, correctWords, mismatches, mismatches.map { userWords[it] })
    }

    /** Checks a single fill-in-the-blank word, tolerating one letter mistake same as the main word-learning engine. */
    fun checkSingleWord(userInput: String, expected: String): AnswerCheck = AnswerChecker.check(userInput, expected)

    private fun tokenize(s: String): List<String> =
        s.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

    private fun stripTrailingPunctuation(word: String): String =
        word.trimEnd('.', '!', '?', ',', ';', ':')
}
