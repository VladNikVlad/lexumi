package com.lexumi.app.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around Android's built-in TextToSpeech engine so words and
 * sentences can be read aloud, with the ability to pick which installed
 * voice to use (point: "додати озвучку слів та речень і вибрати голос").
 */
@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext context: Context,
) : TextToSpeech.OnInitListener {

    private var engine: TextToSpeech? = null

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    private val _voices = MutableStateFlow<List<Voice>>(emptyList())
    val voices: StateFlow<List<Voice>> = _voices

    init {
        engine = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            _voices.value = engine?.voices
                ?.filterNot { it.isNetworkConnectionRequired }
                ?.sortedBy { it.locale.displayName }
                .orEmpty()
            _ready.value = true
        }
    }

    /** Speaks [text] using the voice named [voiceName] if available, otherwise the engine default. */
    fun speak(text: String, voiceName: String?) {
        val tts = engine ?: return
        if (text.isBlank()) return
        if (voiceName != null) {
            val voice = tts.voices?.firstOrNull { it.name == voiceName }
            if (voice != null) tts.voice = voice
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lexumi_${System.currentTimeMillis()}")
    }

    /** Picks the best offline voice available for [locale], preferring higher quality, or null if none installed. */
    fun bestVoiceFor(locale: java.util.Locale): Voice? =
        _voices.value
            .filter { it.locale.language == locale.language && !it.isNetworkConnectionRequired }
            .maxByOrNull { it.quality }

    fun shutdown() {
        engine?.shutdown()
        engine = null
    }
}
