package com.lexumi.app.presentation.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.lexumi.app.presentation.theme.LexumiError

/**
 * Highlights the minimal differing segment between [typed] and [correct] in
 * red, within the correct word — e.g. typed "aple" vs correct "apple" shows
 * "a" + red "p" + "ple". Falls back to highlighting the whole word if the
 * two share no useful common prefix/suffix to anchor on.
 */
fun highlightWordDiff(typed: String, correct: String): AnnotatedString {
    val maxCommon = minOf(typed.length, correct.length)
    var prefixLen = 0
    while (prefixLen < maxCommon && typed[prefixLen].equals(correct[prefixLen], ignoreCase = true)) prefixLen++

    var suffixLen = 0
    val remaining = maxCommon - prefixLen
    while (suffixLen < remaining &&
        typed[typed.length - 1 - suffixLen].equals(correct[correct.length - 1 - suffixLen], ignoreCase = true)
    ) suffixLen++

    val diffStart = prefixLen
    val diffEnd = (correct.length - suffixLen).coerceAtLeast(diffStart)

    return buildAnnotatedString {
        append(correct.substring(0, diffStart))
        withStyle(SpanStyle(color = LexumiError, fontWeight = FontWeight.Bold)) {
            append(correct.substring(diffStart, diffEnd))
        }
        append(correct.substring(diffEnd))
    }
}
