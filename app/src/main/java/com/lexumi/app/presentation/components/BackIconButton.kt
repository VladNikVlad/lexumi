package com.lexumi.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import com.lexumi.app.presentation.theme.LexumiIndigo

@Composable
fun BackIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        // zIndex keeps this above any scrollable content placed after it in
        // the same Box — otherwise a full-size scrollable Column drawn on
        // top silently swallows the touch even where it's visually empty.
        // statusBarsPadding() keeps it below the system status bar/clock.
        modifier = modifier
            .zIndex(10f)
            .statusBarsPadding()
            .background(Color.White.copy(alpha = 0.6f), CircleShape),
    ) {
        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Назад", tint = LexumiIndigo)
    }
}
