package com.lexumi.app.domain.usecase

import com.lexumi.app.domain.model.AnswerCheck
import com.lexumi.app.domain.model.Word
import com.lexumi.app.domain.repository.WordRepository
import javax.inject.Inject
import kotlin.random.Random

const val STREAK_TO_PROMOTE = 5   // level 0 -> level 1
const val SCORE_TO_MASTER = 10.0  // level 1 "mastered" threshold (hidden stat)

/** True when the word should be asked in <term -> translation> direction, false for reverse. */
fun Word.askTermFirst(): Boolean =
    if (score >= SCORE_TO_MASTER) Random.nextBoolean() else true

/**
 * Builds the multiple-choice options for a level-0 word: the correct
 * translation plus three random distractors from the same topic.
 */
class BuildMultipleChoiceUseCase @Inject constructor(private val wordRepository: WordRepository) {
    suspend operator fun invoke(word: Word, askTermFirst: Boolean): List<String> {
        val pool = wordRepository.getWords(word.topicId).filter { it.id != word.id }
        val correct = if (askTermFirst) word.translation else word.term
        val distractorsSource = pool.map { if (askTermFirst) it.translation else it.term }.distinct()
        val distractors = distractorsSource.shuffled().take(3)
        val options = (distractors + correct).toMutableList()
        // pad with placeholders if the topic doesn't yet have 3 other words
        while (options.size < 4) options.add(0, "—")
        return options.shuffled()
    }
}

/**
 * Applies one answer (multiple-choice at level 0, or typed text at level 1)
 * to a word's hidden score/level state, exactly as described in point 20:
 * 5 correct in a row promotes to level 1; a wrong answer resets the streak;
 * at level 1 a correct answer is worth 1 point, a one-letter typo 0.5.
 */
class SubmitWordAnswerUseCase @Inject constructor(private val wordRepository: WordRepository) {

    suspend fun submitChoice(word: Word, wasCorrect: Boolean): Word {
        val updated = if (wasCorrect) {
            val newStreak = word.correctStreak + 1
            if (newStreak >= STREAK_TO_PROMOTE) {
                word.copy(level = 1, correctStreak = 0, timesSeen = word.timesSeen + 1)
            } else {
                word.copy(correctStreak = newStreak, timesSeen = word.timesSeen + 1)
            }
        } else {
            word.copy(correctStreak = 0, timesSeen = word.timesSeen + 1)
        }
        wordRepository.updateWord(updated)
        return updated
    }

    suspend fun submitTypedAnswer(word: Word, userInput: String, expected: String): Pair<Word, AnswerCheck> {
        val result = AnswerChecker.check(userInput, expected)
        val delta = when (result) {
            is AnswerCheck.Correct -> 1.0
            is AnswerCheck.OneLetterTypo -> 0.5
            is AnswerCheck.Wrong -> 0.0
        }
        val updated = word.copy(score = word.score + delta, timesSeen = word.timesSeen + 1)
        wordRepository.updateWord(updated)
        return updated to result
    }

    suspend fun toggleReviewList(word: Word, addToReview: Boolean): Word {
        val updated = word.copy(inReviewList = addToReview)
        wordRepository.updateWord(updated)
        return updated
    }
}

/**
 * Picks the words for one learning session: brand-new / least-seen words are
 * prioritised first, the rest is random, and the list length follows the
 * user's "words per session" setting (default 10, point 20 & profile settings).
 */
class GetSessionWordsUseCase @Inject constructor(private val wordRepository: WordRepository) {
    suspend operator fun invoke(topicId: Long, wordsPerSession: Int): List<Word> {
        val all = wordRepository.getWords(topicId)
        val notMastered = all.filter { it.score < SCORE_TO_MASTER }
        val sorted = notMastered.sortedBy { it.timesSeen }
        val leastSeen = sorted.take(wordsPerSession)
        return leastSeen.shuffled()
    }
}
