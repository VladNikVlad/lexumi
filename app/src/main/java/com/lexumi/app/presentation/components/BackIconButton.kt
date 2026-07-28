package com.lexumi.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.lexumi.app.presentation.theme.LexumiIndigo

@Composable
fun BackIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier.background(Color.White.copy(alpha = 0.6f), CircleShape),
    ) {
        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Назад", tint = LexumiIndigo)
    }
}
