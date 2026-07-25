package com.lexumi.app.util

import android.media.AudioManager
import android.media.ToneGenerator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Short correct/incorrect chime played after each answer, using Android's
 * built-in tone generator — no sound files to bundle, works fully offline.
 */
@Singleton
class SoundFeedbackPlayer @Inject constructor() {

    private val toneGenerator: ToneGenerator? =
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        } catch (e: RuntimeException) {
            null // some devices/emulators have no audio output; fail silently
        }

    fun playCorrect() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 180)
    }

    fun playWrong() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 220)
    }
}
