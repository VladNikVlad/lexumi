package com.lexumi.app.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.lexumi.app.R

@Composable
fun LexumiLogo(modifier: Modifier = Modifier, width: androidx.compose.ui.unit.Dp = 220.dp) {
    Image(
        painter = painterResource(id = R.drawable.lexumi_logo_full),
        contentDescription = "Lexumi",
        modifier = modifier.width(width),
    )
}

@Composable
fun LexumiMark(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 64.dp) {
    Image(
        painter = painterResource(id = R.drawable.lexumi_mark),
        contentDescription = "Lexumi",
        modifier = modifier.width(size),
    )
}
