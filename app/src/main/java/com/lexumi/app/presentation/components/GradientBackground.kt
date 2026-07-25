package com.lexumi.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.lexumi.app.presentation.theme.LexumiBgEnd
import com.lexumi.app.presentation.theme.LexumiBgStart
import com.lexumi.app.presentation.theme.LexumiPurpleLight
import com.lexumi.app.presentation.theme.LexumiTealLight

/**
 * The soft lilac-to-teal gradient with round translucent "blobs" used across
 * every screen of Lexumi (see design mockups).
 */
@Composable
fun GradientBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(LexumiBgStart, LexumiPurpleLight.copy(alpha = 0.25f), LexumiBgEnd),
                )
            )
    ) {
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(x = (-60).dp, y = (-40).dp)
                .size(160.dp)
                .clip(CircleShape)
                .background(LexumiPurpleLight.copy(alpha = 0.35f))
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = (-30).dp)
                .size(140.dp)
                .clip(CircleShape)
                .background(LexumiPurpleLight.copy(alpha = 0.3f))
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .size(180.dp)
                .clip(CircleShape)
                .background(LexumiTealLight.copy(alpha = 0.35f))
        )
        Box(Modifier.fillMaxSize()) { content() }
    }
}
