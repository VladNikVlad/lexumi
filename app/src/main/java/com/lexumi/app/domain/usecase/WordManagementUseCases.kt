package com.lexumi.app.domain.usecase

import com.lexumi.app.domain.model.Word
import com.lexumi.app.domain.repository.WordRepository
import javax.inject.Inject

/** Lets the user fix a word's text, swap/add/remove its picture, or change its rule — without
 * resetting learning progress. Since a word can be shared across topics, the term/image/rule
 * change always applies to the shared word; the translation follows the topic-vs-shared rule
 * (see [com.lexumi.app.data.repository.WordRepositoryImpl.editWord]). */
class EditWordUseCase @Inject constructor(private val repo: WordRepository) {
    suspend operator fun invoke(topicId: Long, word: Word, term: String, translation: String, imagePath: String?, ruleId: Long?): AddResult {
        val trimmedTerm = term.trim()
        if (trimmedTerm.isEmpty() || translation.isBlank()) return AddResult.Blank
        val existing = repo.findByLanguageAndTerm(word.languageId, trimmedTerm)
        if (existing != null && existing.id != word.id) return AddResult.AlreadyExists
        repo.editWord(topicId, word.id, trimmedTerm, translation.trim(), imagePath, ruleId)
        return AddResult.Success(word.id)
    }
}

/** "Додати локальний переклад" — forks this topic's translation away from the shared default. */
class ForkWordTranslationUseCase @Inject constructor(private val repo: WordRepository) {
    suspend operator fun invoke(topicId: Long, word: Word, translation: String) {
        if (translation.isNotBlank()) repo.forkTranslation(topicId, word.id, translation.trim())
    }
}

class DeleteWordUseCase @Inject constructor(private val repo: WordRepository) {
    suspend operator fun invoke(word: Word, topicId: Long) = repo.deleteWord(word, topicId)
}
