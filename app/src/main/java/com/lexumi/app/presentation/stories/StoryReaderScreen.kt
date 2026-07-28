package com.lexumi.app.presentation.stories

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.presentation.components.BackIconButton
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.PillActionButton

@Composable
fun StoryReaderScreen(
    onBack: () -> Unit,
    viewModel: StoryReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val story = state.story

    GradientBackground {
        BackIconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(20.dp))
        if (story == null) return@GradientBackground
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(story.name, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            if (state.rules.isNotEmpty()) {
                Text("Пов'язані правила: " + state.rules.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.55f)), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (state.showTranslation) (story.translation ?: story.text) else story.text,
                    modifier = Modifier.padding(20.dp),
                )
            }
            Spacer(Modifier.height(20.dp))

            if (story.translation != null) {
                PillActionButton(
                    text = if (state.showTranslation) "Показати оригінал" else "Показати переклад",
                    icon = Icons.Filled.Translate,
                    onClick = { viewModel.toggleTranslation() },
                )
            }
        }
    }
}
