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
            // If the saved voice name is no longer installed, we deliberately do NOT touch
            // tts.voice here — leaving it as-is would keep whatever language was last spoken
            // (see the locale overload below for why that's the wrong default).
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lexumi_${System.currentTimeMillis()}")
    }

    /** Speaks [text] in whatever installed voice best matches [locale] — used where a specific
     * piece of text is in a *different* language than the topic's assigned voice, e.g. the
     * native-language side of the "Знайти пару" matching game. Falls back through progressively
     * looser matches rather than silently keeping whatever voice was last active — reusing the
     * wrong-language voice (e.g. reading Ukrainian with the Spanish voice) sounds far worse than
     * a slightly-lower-quality but at least correct-language one. */
    fun speak(text: String, locale: java.util.Locale) {
        val tts = engine ?: return
        if (text.isBlank()) return
        val voice = bestVoiceFor(locale)
            ?: tts.voices?.filter { it.locale.language == locale.language }?.maxByOrNull { it.quality }
        when {
            voice != null -> tts.voice = voice
            else -> tts.language = locale // let the engine pick anything it can for this language, offline or not
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lexumi_${System.currentTimeMillis()}")
    }

    /** Picks the best offline voice available for [locale], preferring higher quality, or null if none installed. */
    fun bestVoiceFor(locale: java.util.Locale): Voice? =
        _voices.value
            .filter { it.locale.language == locale.language && !it.isNetworkConnectionRequired }
            .maxByOrNull { it.quality }

    /** The locale of the voice named [voiceName], if it's an installed one — used to tell the
     * speech recognizer which language to listen for when practicing a word/sentence out loud. */
    fun localeFor(voiceName: String?): java.util.Locale? =
        voiceName?.let { name -> _voices.value.firstOrNull { it.name == name }?.locale }

    fun shutdown() {
        engine?.shutdown()
        engine = null
    }
}
