package com.lexumi.app.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around Android's on-device speech recognizer for the
 * "say the word/sentence out loud" mastery practice: once a word or
 * sentence is fully learned, the app stops asking for typed answers and
 * asks the user to speak it instead.
 *
 * One attempt = one system listening session, started once and left alone —
 * no restarting mid-attempt. An earlier version tried to "extend" the
 * leading silence by destroying and recreating the recognizer when it gave
 * up too early; that raced unpredictably against the device's own internal
 * timeout (different on every OEM) and caused the exact "beep-beep-beep,
 * mic keeps turning on and off" experience it was trying to prevent. If more
 * breathing room is needed before listening starts, delay the call to
 * [listenOnce] itself (see [initialDelayMillis]) rather than fight the
 * recognizer once it's already running.
 *
 * [onDebug] reports what's happening as it happens (ready/listening/partial
 * hypotheses/errors) — wire it up in the UI while testing to see exactly why
 * recognition isn't picking anything up (no mic permission, no network for a
 * network-based recognizer, the device's language pack not installed, etc.).
 */
@Singleton
class VoiceRecognizerManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingStart: Runnable? = null

    val isAvailable: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    /**
     * Starts listening once for [locale] (falls back to the device default
     * if null/unavailable). Calls [onResult] exactly once with the best
     * recognized phrase, or an empty string if nothing was ever heard.
     *
     * [initialDelayMillis] delays the actual start of listening — e.g. to
     * give the user a moment to read the word/take a breath before the mic
     * opens, without touching how long the recognizer itself waits once
     * it's running.
     *
     * [onPartial] fires repeatedly with the current best guess *while* the
     * user is still talking — used both for the live debug line and for
     * early-stopping (see [stopAndFinalize]) the instant a partial guess
     * already matches the expected answer, instead of always waiting out
     * the full trailing-silence timeout. [onDebug] reports status/errors as
     * short human-readable lines. Calling this again while already
     * listening (or waiting on [initialDelayMillis]) cancels the previous attempt.
     */
    fun listenOnce(
        locale: Locale?,
        initialDelayMillis: Long = 0,
        onPartial: (String) -> Unit = {},
        onDebug: (String) -> Unit = {},
        onResult: (String) -> Unit,
    ) {
        stop()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onDebug("На цьому пристрої немає сервісу розпізнавання мовлення (немає Google App / STT).")
            onResult("")
            return
        }

        fun start() {
            var finished = false
            // Some recognizer services (notably on certain OEM builds) occasionally return an
            // empty final result even though they clearly heard something — the partial
            // hypotheses along the way had real text. Keep the last non-empty partial as a
            // fallback so a good guess isn't thrown away just because the "final" pass came back empty.
            var lastPartial = ""

            fun finish(text: String) {
                if (finished) return
                finished = true
                onResult(text)
                recognizer?.destroy()
                recognizer = null
            }
            fun finishWithFallback(reason: String) {
                if (lastPartial.isNotBlank()) {
                    onDebug("$reason Використав останнє почуте: «$lastPartial»")
                    finish(lastPartial)
                } else {
                    onDebug(reason)
                    finish("")
                }
            }

            val r = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer = r
            r.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = onDebug("Готовий, кажи…")
                override fun onBeginningOfSpeech() = onDebug("Чую мовлення…")
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = onDebug("Обробляю…")
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull()
                    if (!best.isNullOrBlank()) {
                        lastPartial = best
                        onPartial(best)
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
                override fun onResults(results: Bundle) {
                    val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull().orEmpty()
                    if (best.isBlank()) {
                        finishWithFallback("Остаточний результат порожній (буває на деяких пристроях).")
                    } else {
                        onDebug("Розпізнано: «$best»")
                        finish(best)
                    }
                }
                override fun onError(error: Int) {
                    if (finished) return
                    if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        finishWithFallback("Помилка: ${errorMessage(error)}.")
                    } else {
                        onDebug("Помилка: ${errorMessage(error)}")
                        finish("")
                    }
                }
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                // Silence AFTER the user has started talking — ends listening and finalizes the answer.
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000)
                if (locale != null) putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            }
            r.startListening(intent)
        }

        if (locale != null) onDebug("Мова розпізнавання: ${locale.toLanguageTag()}")
        else onDebug("Мова розпізнавання: за замовчуванням (не визначено голос теми)")

        if (initialDelayMillis <= 0) {
            start()
        } else {
            onDebug("Хвилинку…")
            val task = Runnable { pendingStart = null; start() }
            pendingStart = task
            mainHandler.postDelayed(task, initialDelayMillis)
        }
    }

    /**
     * Ends listening right now and lets the recognizer finalize whatever it has heard so far —
     * used for early-stopping the instant a partial result already matches the expected answer,
     * instead of always waiting out the trailing-silence timeout. This still goes through the
     * normal [RecognitionListener.onResults] callback, unlike [stop] which discards everything.
     */
    fun stopAndFinalize() {
        recognizer?.stopListening()
    }

    /**
     * Checks a recognized phrase against a list of acceptable answers. Tolerant on two axes,
     * since speech recognition is noisy: (1) extra words the recognizer may have tacked on —
     * tries the full phrase first, then progressively shorter trailing windows of it, e.g. for a
     * 2-word answer it tries the last 1 through 5 words of what was heard; for a 3-word answer,
     * up to the last 6 — so "la pizarra" still matches "eh la pizarra"; and (2) a handful of
     * misheard letters within a candidate — fuzzy-matched by edit distance rather than requiring
     * an exact match, since chasing perfect pronunciation recognition isn't realistic.
     */
    fun matches(heardRaw: String, acceptableAnswers: List<String>): Boolean {
        val cleanedAnswers = acceptableAnswers.map { it.trim() }.filter { it.isNotBlank() }
        if (cleanedAnswers.isEmpty()) return false
        val full = heardRaw.trim()
        if (full.isBlank()) return false
        if (cleanedAnswers.any { fuzzyEquals(it, full) }) return true

        val heardWords = full.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (heardWords.isEmpty()) return false
        val answerWordCount = cleanedAnswers.maxOf { it.split(Regex("\\s+")).size }
        val maxWindow = (answerWordCount + 3).coerceAtMost(heardWords.size)
        for (window in 1..maxWindow) {
            val candidate = heardWords.takeLast(window).joinToString(" ")
            if (cleanedAnswers.any { fuzzyEquals(it, candidate) }) return true
        }
        return false
    }

    /** Case/diacritic-insensitive comparison, tolerating a few misheard letters — the tolerance
     * scales with word length so short words still need to be close to exact. */
    private fun fuzzyEquals(a: String, b: String): Boolean {
        val na = normalize(a)
        val nb = normalize(b)
        if (na.isBlank() || nb.isBlank()) return false
        if (na == nb) return true
        val maxLen = maxOf(na.length, nb.length)
        val tolerance = when {
            maxLen <= 3 -> 0   // very short words: must be exact once normalized
            maxLen <= 5 -> 1
            maxLen <= 8 -> 2
            else -> 3
        }
        return levenshtein(na, nb) <= tolerance
    }

    private fun normalize(text: String): String {
        val decomposed = java.text.Normalizer.normalize(text.trim().lowercase(Locale.ROOT), java.text.Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{Mn}+"), "") // strip accents (á -> a, etc.)
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        return dp[a.length][b.length]
    }

    /** Cancels a pending delayed start (if any) and/or destroys the active recognizer without
     * finalizing — no result callback fires. Use [stopAndFinalize] instead if a result is wanted. */
    fun stop() {
        pendingStart?.let { mainHandler.removeCallbacks(it) }
        pendingStart = null
        recognizer?.destroy()
        recognizer = null
    }

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "помилка аудіозапису"
        SpeechRecognizer.ERROR_CLIENT -> "помилка клієнта (спробуй ще раз)"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "немає дозволу на мікрофон"
        SpeechRecognizer.ERROR_NETWORK -> "немає інтернету (потрібен для цього розпізнавача)"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "тайм-аут мережі"
        SpeechRecognizer.ERROR_NO_MATCH -> "нічого не розпізнано — сказане не збіглося з жодним словом"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "розпізнавач зайнятий"
        SpeechRecognizer.ERROR_SERVER -> "помилка сервера розпізнавання"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "тиша — мовлення не почуто"
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "ця мова не підтримується розпізнавачем на пристрої"
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "мовний пакет для розпізнавання не встановлено на пристрої"
        else -> "невідома помилка ($error)"
    }
}
