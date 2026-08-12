package com.lexumi.app.presentation.components

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

/**
 * A full-screen-friendly image that can be pinch-zoomed and panned — for
 * viewing a photographed rule/textbook page where the small preview
 * thumbnail elsewhere in the app isn't legible. Double-tap-to-reset isn't
 * included on purpose: a stray double-tap while reading shouldn't suddenly
 * snap the zoom back.
 */
@Composable
fun ZoomableImage(model: Any?, contentDescription: String?, modifier: Modifier = Modifier) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier.fillMaxSize().pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                val newScale = (scale * zoom).coerceIn(1f, 6f)
                // Panning is only meaningful once zoomed in; snap back to
                // centered when the pinch returns to 1x.
                val maxOffsetX = (size.width * (newScale - 1) / 2f).coerceAtLeast(0f)
                val maxOffsetY = (size.height * (newScale - 1) / 2f).coerceAtLeast(0f)
                offsetX = if (newScale <= 1f) 0f else (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                offsetY = if (newScale <= 1f) 0f else (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                scale = newScale
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale, scaleY = scale,
                    translationX = offsetX, translationY = offsetY,
                ),
        )
    }
}
