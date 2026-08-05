package com.lexumi.app.domain.usecase

import com.lexumi.app.domain.model.Sentence
import kotlin.random.Random

/** Once a sentence is answered correctly often enough, occasionally flip the
 * direction: show the translation and ask for the original sentence back,
 * instead of always original -> translation. Mirrors [askTermFirst] for words. */
private const val SENTENCE_REVERSAL_THRESHOLD = 5

fun Sentence.askOriginalFirst(): Boolean =
    if (totalCorrect >= SENTENCE_REVERSAL_THRESHOLD) Random.nextBoolean() else true
