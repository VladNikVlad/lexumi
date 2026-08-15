package com.lexumi.app.presentation.audio

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
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
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.lexumi.app.presentation.components.BackIconButton
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.PillActionButton
import com.lexumi.app.presentation.theme.LexumiOutline
import kotlinx.coroutines.delay

/** 100% down to 30%, in steps of 10 — "уповільнення на 10-70%". */
private val SPEED_PRESETS = listOf(1.0f, 0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.4f, 0.3f)

private fun formatMillis(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
fun AudioPlayerScreen(
    onBack: () -> Unit,
    viewModel: AudioPlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val dialog = state.dialog

    GradientBackground {
        BackIconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(20.dp))
        if (dialog == null) return@GradientBackground

        val player = remember(dialog.audioPath) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(dialog.audioPath)))
                prepare()
            }
        }
        DisposableEffect(Unit) { onDispose { player.release() } }

        var isPlaying by remember { mutableStateOf(false) }
        var durationMs by remember { mutableLongStateOf(0L) }
        var positionMs by remember { mutableLongStateOf(0L) }
        var speed by remember { mutableFloatStateOf(1.0f) }
        var speedMenuExpanded by remember { mutableStateOf(false) }
        // While the user is dragging the seek bar, show the drag position instead of the
        // player's own (so the thumb doesn't fight back against the finger).
        var draggingTo by remember { mutableStateOf<Float?>(null) }

        DisposableEffect(player) {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) durationMs = player.duration.coerceAtLeast(0L)
                }
            }
            player.addListener(listener)
            onDispose { player.removeListener(listener) }
        }
        // Polling is simpler and plenty accurate for a seek bar than wiring a frame callback.
        LaunchedEffect(player) {
            while (true) {
                if (draggingTo == null) positionMs = player.currentPosition
                delay(250)
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(28.dp).padding(top = 72.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(dialog.name, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(28.dp))

            // --- player card ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.55f)),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val sliderValue = draggingTo ?: positionMs.toFloat()
                    Slider(
                        value = sliderValue,
                        valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                        onValueChange = { draggingTo = it },
                        onValueChangeFinished = {
                            draggingTo?.let { player.seekTo(it.toLong()) }
                            draggingTo = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatMillis(sliderValue.toLong()), style = MaterialTheme.typography.bodySmall, color = LexumiOutline)
                        Text(formatMillis(durationMs), style = MaterialTheme.typography.bodySmall, color = LexumiOutline)
                    }
                    Spacer(Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        IconButton(onClick = { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)) }) {
                            Icon(Icons.Filled.Replay10, contentDescription = "Назад 10с")
                        }
                        IconButton(
                            onClick = { if (isPlaying) player.pause() else player.play() },
                            modifier = Modifier.size(64.dp).background(Color.White.copy(alpha = 0.7f), CircleShape),
                        ) {
                            Icon(
                                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Пауза" else "Слухати",
                                modifier = Modifier.size(36.dp),
                            )
                        }
                        IconButton(onClick = { player.seekTo((player.currentPosition + 10_000).coerceAtMost(durationMs)) }) {
                            Icon(Icons.Filled.Forward10, contentDescription = "Вперед 10с")
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    Box {
                        TextButton(onClick = { speedMenuExpanded = true }) {
                            Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Швидкість: ${(speed * 100).toInt()}%")
                        }
                        DropdownMenu(expanded = speedMenuExpanded, onDismissRequest = { speedMenuExpanded = false }) {
                            SPEED_PRESETS.forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text("${(preset * 100).toInt()}%") },
                                    onClick = {
                                        speed = preset
                                        player.playbackParameters = PlaybackParameters(preset)
                                        speedMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            // --- below the player: translation, rules, questions ---
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
}
