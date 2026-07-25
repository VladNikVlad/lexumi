package com.lexumi.app.presentation.video

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.PillActionButton

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun VideoPlayerScreen(
    viewModel: VideoPlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val answers = remember { mutableStateMapOf<Long, Boolean>() }

    val video = state.video ?: return

    GradientBackground {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(video.name, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            PillActionButton(
                text = "Дивитися на YouTube",
                icon = Icons.Filled.PlayCircle,
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(video.youtubeUrl)))
                },
            )
            Spacer(Modifier.height(20.dp))

            if (state.rules.isNotEmpty()) {
                Text("Пов'язані правила: " + state.rules.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
            }

            if (video.originalText != null) {
                PillActionButton(
                    text = if (state.showTranslation) "Показати оригінал" else "Показати переклад",
                    icon = Icons.Filled.Translate,
                    onClick = { viewModel.toggleTranslation() },
                )
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.55f)), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (state.showTranslation) (video.translationText ?: "") else video.originalText,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

            if (state.questions.isNotEmpty()) {
                Text("Перевір себе:", style = MaterialTheme.typography.titleMedium)
                state.questions.forEach { q ->
                    Spacer(Modifier.height(8.dp))
                    Text(q.questionText, style = MaterialTheme.typography.bodyMedium)
                    Row {
                        FilterChip(
                            selected = answers[q.id] == true,
                            onClick = { answers[q.id] = true },
                            label = { Text("Так") },
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        FilterChip(
                            selected = answers[q.id] == false,
                            onClick = { answers[q.id] = false },
                            label = { Text("Ні") },
                        )
                    }
                    if (answers.containsKey(q.id)) {
                        val correct = answers[q.id] == q.correctBoolean
                        Text(
                            if (correct) "Правильно ✓" else "Неправильно",
                            color = if (correct) com.lexumi.app.presentation.theme.LexumiSuccess else com.lexumi.app.presentation.theme.LexumiError,
                        )
                    }
                }
            }
        }
    }
}
