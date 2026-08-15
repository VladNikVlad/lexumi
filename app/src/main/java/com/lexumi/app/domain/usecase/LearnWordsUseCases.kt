package com.lexumi.app.domain.usecase

import com.lexumi.app.domain.model.AnswerCheck
import com.lexumi.app.domain.model.Word
import com.lexumi.app.domain.repository.WordRepository
import javax.inject.Inject

/** Every phase of the mastery ladder needs this many correct answers *in a row* to advance. */
const val STREAK_TO_ADVANCE = 5

/**
 * Builds the multiple-choice options for a rating-0 word: the correct
 * translation plus three random distractors from the same topic. Options are
 * cleaned with [TranslationParser] so a field with several "/" variants or a
 * "(...)" explanation still shows as one short, unambiguous button.
 */
class BuildMultipleChoiceUseCase @Inject constructor(private val wordRepository: WordRepository) {
    suspend operator fun invoke(word: Word): List<String> {
        val pool = wordRepository.getWords(word.topicId).filter { it.id != word.id }
        val correct = TranslationParser.displayPrimary(word.translation)
        val distractorsSource = pool
            .map { TranslationParser.displayPrimary(it.translation) }
            .filter { it != correct }
            .distinct()
        val distractors = distractorsSource.shuffled().take(3)
        val options = (distractors + correct).toMutableList()
        // pad with placeholders if the topic doesn't yet have 3 other words
        while (options.size < 4) options.add(0, "—")
        return options.shuffled()
    }
}

/**
 * Applies one answer to a word's position on the mastery ladder:
 * 0 new (multiple choice) -> 1 typed, both directions -> 2 say-it-aloud
 * cards -> 3 hear-only -> 4 mastered. Every phase needs [STREAK_TO_ADVANCE]
 * correct answers *in a row*; any miss resets that phase's streak to zero.
 * A one-letter typo now counts as fully correct — only a genuine miss breaks
 * a streak.
 */
class SubmitWordAnswerUseCase @Inject constructor(private val wordRepository: WordRepository) {

    private fun Word.withStatsUpdate(wasCorrect: Boolean): Word {
        val newStreak = if (wasCorrect) currentStatsStreak + 1 else 0
        return copy(
            totalCorrect = totalCorrect + if (wasCorrect) 1 else 0,
            currentStatsStreak = newStreak,
            bestStreak = maxOf(bestStreak, newStreak),
            timesSeen = timesSeen + 1,
        )
    }

    /** Rating 0: multiple choice. 5 correct in a row -> rating 1. */
    suspend fun submitChoice(word: Word, wasCorrect: Boolean): Word {
        val withStats = word.withStatsUpdate(wasCorrect)
        val newStreak = if (wasCorrect) withStats.correctStreak + 1 else 0
        val updated = if (newStreak >= STREAK_TO_ADVANCE) {
            withStats.copy(rating = 1, correctStreak = 0, typedStreak = 0, typedReverseActive = false)
        } else {
            withStats.copy(correctStreak = newStreak)
        }
        wordRepository.updateWord(updated)
        return updated
    }

    /** Rating 1 (typed, direct then reverse) and rating 3 (hear-only, typed or spoken). */
    suspend fun submitTypedAnswer(word: Word, userInput: String, expected: String): Pair<Word, AnswerCheck> {
        val result = AnswerChecker.check(userInput, expected)
        val wasCorrect = result !is AnswerCheck.Wrong // a one-letter typo now counts as correct
        val withStats = word.withStatsUpdate(wasCorrect)
        val updated = when (word.rating) {
            1 -> advanceTypedPhase(withStats, wasCorrect)
            3 -> advanceHearOnlyPhase(withStats, wasCorrect)
            else -> withStats
        }
        wordRepository.updateWord(updated)
        return updated to result
    }

    /** Rating 2's cards round: a spoken answer — recognized speech is checked by the caller. */
    suspend fun submitVoiceCardAnswer(word: Word, wasCorrect: Boolean): Word {
        val withStats = word.withStatsUpdate(wasCorrect)
        val newStreak = if (wasCorrect) withStats.voiceStreak + 1 else 0
        val updated = if (newStreak >= STREAK_TO_ADVANCE) {
            withStats.copy(rating = 3, voiceStreak = 0, finalStreak = 0)
        } else {
            withStats.copy(voiceStreak = newStreak)
        }
        wordRepository.updateWord(updated)
        return updated
    }

    private fun advanceTypedPhase(word: Word, wasCorrect: Boolean): Word {
        val newStreak = if (wasCorrect) word.typedStreak + 1 else 0
        if (newStreak < STREAK_TO_ADVANCE) return word.copy(typedStreak = newStreak)
        return if (!word.typedReverseActive) {
            // direct phase just finished — start the reverse phase
            word.copy(typedStreak = 0, typedReverseActive = true)
        } else {
            // reverse phase just finished — on to the cards round
            word.copy(rating = 2, typedStreak = 0, typedReverseActive = false, voiceStreak = 0, finalStreak = 0)
        }
    }

    private fun advanceHearOnlyPhase(word: Word, wasCorrect: Boolean): Word {
        val newStreak = if (wasCorrect) word.finalStreak + 1 else 0
        return if (newStreak >= STREAK_TO_ADVANCE) {
            word.copy(rating = 4, finalStreak = 0)
        } else {
            word.copy(finalStreak = newStreak)
        }
    }

    suspend fun toggleReviewList(word: Word, addToReview: Boolean): Word {
        val updated = word.copy(inReviewList = addToReview)
        wordRepository.updateWord(updated)
        return updated
    }

    /** "Вже знаю": ratings 0-1 jump straight to the cards round (2); ratings 2-3 jump straight to mastered (4). */
    suspend fun markAsKnown(word: Word): Word {
        val updated = if (word.rating <= 1) {
            word.copy(rating = 2, correctStreak = 0, typedStreak = 0, typedReverseActive = false, voiceStreak = 0, finalStreak = 0)
        } else {
            word.copy(rating = 4, voiceStreak = 0, finalStreak = 0)
        }
        wordRepository.updateWord(updated)
        return updated
    }

    /** From the stats screen: bring a mastered word back for practice, restarting at the cards round. */
    suspend fun repeatMasteredWord(word: Word): Word {
        val updated = word.copy(rating = 2, voiceStreak = 0, finalStreak = 0)
        wordRepository.updateWord(updated)
        return updated
    }
}

/**
 * Picks the words for one learning session's main pass: ratings 0, 1 and 3 —
 * multiple-choice, typed, and hear-only, everything answered inline one word
 * at a time. Rating-2 words are practiced separately in the end-of-session
 * "cards" voice round, and rating-4 (mastered) words are left out entirely.
 * Brand-new / least-seen words are prioritised first, the count follows
 * "words per session", and each picked word is queued [repetitions] times,
 * all shuffled together. Returns word IDs rather than snapshots, since a
 * word's rating/streak can change between its own repeats within the session.
 */
/** What one learning session should practice, decided together so the "words per session"
 * setting is a single shared budget rather than two separate unlimited pools. */
data class WordSessionPlan(
    /** Main-pass queue (ratings 0/1/3 — choice, typed, hear-only), word IDs, already repeated
     * per the "repetitions" setting and shuffled. */
    val mainPassQueue: List<Long>,
    /** Rating-2 words for the end-of-session "say it aloud" cards round — one attempt each, no repeats. */
    val cardsRoundWordIds: List<Long>,
)

/**
 * Picks the words for one learning session out of a single shared budget
 * ("words per session"), split between the main pass (ratings 0/1/3 —
 * multiple-choice, typed, hear-only) and the end-of-session cards round
 * (rating 2 — say it aloud): e.g. 15 words per session with 5 of them
 * already at the cards stage means 5 cards + 10 main-pass words, not 15 of
 * each. Brand-new / least-seen words are prioritised first; rating-4
 * (mastered) words are left out entirely.
 */
class GetSessionWordsUseCase @Inject constructor(private val wordRepository: WordRepository) {
    suspend operator fun invoke(topicId: Long, wordsPerSession: Int, repetitions: Int): WordSessionPlan {
        val all = wordRepository.getWords(topicId)
        val practicable = all.filter { it.rating in 0..3 }
        val chosen = practicable.sortedBy { it.timesSeen }.take(wordsPerSession)
        val mainIds = chosen.filter { it.rating != 2 }.map { it.id }
        val cardIds = chosen.filter { it.rating == 2 }.map { it.id }
        val repeatCount = repetitions.coerceAtLeast(1)
        val mainQueue = buildList { repeat(repeatCount) { addAll(mainIds) } }.shuffled()
        return WordSessionPlan(mainQueue, cardIds)
    }
}
