package com.lexumi.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.zIndex
import com.lexumi.app.presentation.theme.LexumiIndigo

@Composable
fun TopEndIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .zIndex(10f)
            .statusBarsPadding()
            .background(Color.White.copy(alpha = 0.6f), CircleShape),
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = LexumiIndigo)
    }
}
