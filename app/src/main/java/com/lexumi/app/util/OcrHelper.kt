package com.lexumi.app.util

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Extracts text from a photo entirely on the device — free, offline, no
 * server involved. Uses ML Kit's Latin-script recognizer, so it reads
 * words in the language being learned (English, Spanish, etc.) well;
 * Cyrillic text (e.g. a Ukrainian translation) may come out less reliably
 * since it isn't one of the officially supported scripts of this model.
 */
@Singleton
class OcrHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognizeText(uri: Uri): String = suspendCancellableCoroutine { cont ->
        try {
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image)
                .addOnSuccessListener { result -> if (cont.isActive) cont.resume(result.text) }
                .addOnFailureListener { if (cont.isActive) cont.resume("") }
        } catch (e: Exception) {
            if (cont.isActive) cont.resume("")
        }
    }
}
