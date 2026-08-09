package com.lexumi.app.domain.usecase

import com.lexumi.app.domain.model.Sentence

/**
 * Sentence mastery ladder, mirrors words:
 *  0 - text shown in the language being learned, type the native translation
 *  1 - text shown in native language, type the sentence in the language being learned
 *  2 - heard only (TTS, target language), type the native translation
 *  3 - native text shown, answer spoken aloud in the language being learned (voice only)
 *  4 - mastered, excluded from practice
 */

/** True when the target-language original should be shown/heard and the native translation is
 * expected back (ratings 0 and 2); false when the native text is shown and the target-language
 * sentence is expected back (ratings 1 and 3). */
fun Sentence.askOriginalFirst(): Boolean = rating == 0 || rating == 2

/** Rating 2: no text at all — only the TTS reading of the sentence. */
fun Sentence.isAudioOnly(): Boolean = rating == 2

/** Rating 3: the answer must be spoken, not typed. */
fun Sentence.isVoiceOnly(): Boolean = rating == 3
