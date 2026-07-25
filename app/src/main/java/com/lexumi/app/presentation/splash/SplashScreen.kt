package com.lexumi.app.presentation.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.LexumiLogo
import com.lexumi.app.presentation.theme.LexumiIndigo

@Composable
fun SplashScreen(
    onNavigateWelcome: () -> Unit,
    onNavigateLanguageMenu: () -> Unit,
    onNavigateHome: (Long) -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val progress by viewModel.progress.collectAsState()
    val destination by viewModel.destination.collectAsState()

    LaunchedEffect(destination) {
        when (val d = destination) {
            is SplashDestination.Welcome -> onNavigateWelcome()
            is SplashDestination.LanguageMenu -> onNavigateLanguageMenu()
            is SplashDestination.Home -> onNavigateHome(d.languageId)
            SplashDestination.Loading -> Unit
        }
    }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LexumiLogo(width = 260.dp)
            Text(
                text = "Language Learning. Redefined.",
                style = MaterialTheme.typography.bodyMedium,
                color = LexumiIndigo,
                modifier = Modifier.padding(top = 8.dp, bottom = 48.dp),
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = LexumiIndigo,
                trackColor = LexumiIndigo.copy(alpha = 0.15f),
            )
        }
    }
}
