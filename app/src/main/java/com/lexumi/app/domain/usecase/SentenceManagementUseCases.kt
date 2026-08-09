package com.lexumi.app.domain.usecase

import com.lexumi.app.domain.model.Sentence
import com.lexumi.app.domain.repository.SentenceRepository
import javax.inject.Inject

/** Lets the user fix a sentence's text/translations/rules without losing its practice stats. */
class EditSentenceUseCase @Inject constructor(private val repo: SentenceRepository) {
    suspend operator fun invoke(sentence: Sentence, text: String, translations: List<String>, ruleIds: List<Long>): AddResult {
        val trimmedText = text.trim()
        val cleanedTranslations = translations.map { it.trim() }.filter { it.isNotBlank() }
        if (trimmedText.isEmpty() || cleanedTranslations.isEmpty()) return AddResult.Blank
        val duplicate = repo.getSentences(sentence.topicId).any { it.id != sentence.id && it.name.equals(trimmedText, ignoreCase = true) }
        if (duplicate) return AddResult.AlreadyExists
        repo.updateStats(sentence.copy(name = trimmedText, text = trimmedText, translations = cleanedTranslations, ruleIds = ruleIds))
        return AddResult.Success(sentence.id)
    }
}

class DeleteSentenceUseCase @Inject constructor(private val repo: SentenceRepository) {
    suspend operator fun invoke(sentence: Sentence) = repo.deleteSentence(sentence)
}

/**
 * Picks the sentences for one practice session using the very same "words
 * per session" / "repetitions" settings as word learning. Mastered sentences
 * (rating 4) are left out — everything else (ratings 0-3) is practiced
 * inline, one at a time, each rating just changing the direction/format.
 */
class GetSessionSentencesUseCase @Inject constructor(private val repo: SentenceRepository) {
    suspend operator fun invoke(topicId: Long, sentencesPerSession: Int, repetitions: Int): List<Long> {
        val all = repo.getSentences(topicId).filter { !it.known && it.rating < 4 }
        val sorted = all.sortedBy { it.timesSeen }
        val chosenIds = sorted.take(sentencesPerSession).map { it.id }
        val repeatCount = repetitions.coerceAtLeast(1)
        val queue = buildList { repeat(repeatCount) { addAll(chosenIds) } }
        return queue.shuffled()
    }
}

/**
 * Applies one answer to a sentence's position on its mastery ladder
 * (0 -> 1 -> 2 -> 3 -> 4), the sentence equivalent of [SubmitWordAnswerUseCase].
 * Every phase needs [STREAK_TO_ADVANCE] correct answers *in a row*.
 */
class SubmitSentenceAnswerUseCase @Inject constructor(private val repo: SentenceRepository) {

    private fun Sentence.withStatsUpdate(wasCorrect: Boolean): Sentence {
        val newStreak = if (wasCorrect) currentStatsStreak + 1 else 0
        return copy(
            totalCorrect = totalCorrect + if (wasCorrect) 1 else 0,
            currentStatsStreak = newStreak,
            bestStreak = maxOf(bestStreak, newStreak),
            timesSeen = timesSeen + 1,
        )
    }

    suspend fun submit(sentence: Sentence, wasCorrect: Boolean): Sentence {
        val withStats = sentence.withStatsUpdate(wasCorrect)
        val updated = when (sentence.rating) {
            0 -> {
                val streak = if (wasCorrect) withStats.directStreak + 1 else 0
                if (streak >= STREAK_TO_ADVANCE) withStats.copy(rating = 1, directStreak = 0, reverseStreak = 0)
                else withStats.copy(directStreak = streak)
            }
            1 -> {
                val streak = if (wasCorrect) withStats.reverseStreak + 1 else 0
                if (streak >= STREAK_TO_ADVANCE) withStats.copy(rating = 2, reverseStreak = 0, audioStreak = 0)
                else withStats.copy(reverseStreak = streak)
            }
            2 -> {
                val streak = if (wasCorrect) withStats.audioStreak + 1 else 0
                if (streak >= STREAK_TO_ADVANCE) withStats.copy(rating = 3, audioStreak = 0, voiceStreak = 0)
                else withStats.copy(audioStreak = streak)
            }
            3 -> {
                val streak = if (wasCorrect) withStats.voiceStreak + 1 else 0
                if (streak >= STREAK_TO_ADVANCE) withStats.copy(rating = 4, voiceStreak = 0)
                else withStats.copy(voiceStreak = streak)
            }
            else -> withStats
        }
        repo.updateStats(updated)
        return updated
    }

    /** "Вже знаю": ratings 0-1 jump straight to the audio round (2); ratings 2-3 jump straight to mastered (4). */
    suspend fun markAsKnown(sentence: Sentence): Sentence {
        val updated = if (sentence.rating <= 1) {
            sentence.copy(rating = 2, directStreak = 0, reverseStreak = 0, audioStreak = 0, voiceStreak = 0)
        } else {
            sentence.copy(rating = 4, audioStreak = 0, voiceStreak = 0)
        }
        repo.updateStats(updated)
        return updated
    }

    /** From the stats screen: bring a mastered sentence back for practice, restarting at the audio round. */
    suspend fun repeatMastered(sentence: Sentence): Sentence {
        val updated = sentence.copy(rating = 2, audioStreak = 0, voiceStreak = 0)
        repo.updateStats(updated)
        return updated
    }
}
