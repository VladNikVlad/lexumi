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
