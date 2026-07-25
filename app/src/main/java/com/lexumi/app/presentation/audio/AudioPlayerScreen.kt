package com.lexumi.app.presentation.audio

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.PillActionButton

@Composable
fun AudioPlayerScreen(
    viewModel: AudioPlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val dialog = state.dialog ?: return

    val player = remember(dialog.audioPath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(dialog.audioPath)))
            prepare()
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    GradientBackground {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(dialog.name, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(20.dp))

            PillActionButton(
                text = if (state.isPlaying) "Пауза" else "Слухати",
                icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                onClick = {
                    if (state.isPlaying) player.pause() else player.play()
                    viewModel.setPlaying(!state.isPlaying)
                },
            )
            Spacer(Modifier.height(20.dp))

            if (state.rules.isNotEmpty()) {
                Text("Пов'язані правила: " + state.rules.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
            }

            if (dialog.translationText != null) {
                PillActionButton(
                    text = if (state.showTranslation) "Сховати переклад" else "Показати переклад",
                    icon = Icons.Filled.Translate,
                    onClick = { viewModel.toggleTranslation() },
                )
                if (state.showTranslation) {
                    Spacer(Modifier.height(12.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.55f)), modifier = Modifier.fillMaxWidth()) {
                        Text(dialog.translationText, modifier = Modifier.padding(16.dp))
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            if (state.questions.isNotEmpty()) {
                Text("Перевір себе:", style = MaterialTheme.typography.titleMedium)
                state.questions.forEach { q ->
                    Spacer(Modifier.height(8.dp))
                    Text(q.questionText, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
