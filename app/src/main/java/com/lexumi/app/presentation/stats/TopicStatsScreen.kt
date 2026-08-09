package com.lexumi.app.presentation.stats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.presentation.components.BackIconButton
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.theme.LexumiOutline
import com.lexumi.app.presentation.theme.LexumiSuccess

/** Short, friendly label for a mastery rating — same ladder for words and sentences. */
private fun ratingLabel(rating: Int): String = when (rating) {
    0 -> "Новий"
    1 -> "Письмово"
    2 -> "Картки"
    3 -> "На слух"
    else -> "Вивчено"
}

@Composable
fun TopicStatsScreen(
    onBack: () -> Unit,
    viewModel: TopicStatsViewModel = hiltViewModel(),
) {
    val wordRows by viewModel.rows.collectAsState()
    val sentenceRows by viewModel.sentenceRows.collectAsState()
    var tab by remember { mutableStateOf(0) }
    var repeatWordId by remember { mutableStateOf<Long?>(null) }
    var repeatSentenceId by remember { mutableStateOf<Long?>(null) }

    GradientBackground {
        BackIconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(20.dp))
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp).padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Статистика", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            TabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Слова") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Речення") })
            }
            Spacer(Modifier.height(12.dp))

            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                if (tab == 0) {
                    if (wordRows.isEmpty()) {
                        Text("Ще немає даних — почни вчити слова цієї теми.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        StatsHeaderRow()
                        wordRows.forEach { row ->
                            StatRow(
                                title = row.term, subtitle = row.translation, rating = row.rating,
                                timesSeen = row.timesSeen, totalCorrect = row.totalCorrect, bestStreak = row.bestStreak,
                                accuracyPercent = row.accuracyPercent,
                                onClick = if (row.rating == 4) ({ repeatWordId = row.id }) else null,
                            )
                        }
                    }
                } else {
                    if (sentenceRows.isEmpty()) {
                        Text("Ще немає даних — почни вчити речення цієї теми.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        StatsHeaderRow()
                        sentenceRows.forEach { row ->
                            StatRow(
                                title = row.text, subtitle = row.translation, rating = row.rating,
                                timesSeen = row.timesSeen, totalCorrect = row.totalCorrect, bestStreak = row.bestStreak,
                                accuracyPercent = row.accuracyPercent,
                                onClick = if (row.rating == 4) ({ repeatSentenceId = row.id }) else null,
                            )
                        }
                    }
                }
            }
        }

        if (repeatWordId != null) {
            AlertDialog(
                onDismissRequest = { repeatWordId = null },
                title = { Text("Повторити це слово?") },
                text = { Text("Воно вже вивчене. Повторення поверне його до практики (картки).") },
                confirmButton = {
                    TextButton(onClick = { viewModel.repeatWord(repeatWordId!!); repeatWordId = null }) { Text("Так, повторити") }
                },
                dismissButton = { TextButton(onClick = { repeatWordId = null }) { Text("Скасувати") } },
            )
        }
        if (repeatSentenceId != null) {
            AlertDialog(
                onDismissRequest = { repeatSentenceId = null },
                title = { Text("Повторити це речення?") },
                text = { Text("Воно вже вивчене. Повторення поверне його до практики (аудіо).") },
                confirmButton = {
                    TextButton(onClick = { viewModel.repeatSentence(repeatSentenceId!!); repeatSentenceId = null }) { Text("Так, повторити") }
                },
                dismissButton = { TextButton(onClick = { repeatSentenceId = null }) { Text("Скасувати") } },
            )
        }
    }
}

@Composable
private fun StatsHeaderRow() {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Text("Слово / речення", modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelLarge)
        Text("Рейтинг", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
        Text("Серія", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
        Text("%", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun StatRow(
    title: String,
    subtitle: String,
    rating: Int,
    timesSeen: Int,
    totalCorrect: Int,
    bestStreak: Int,
    accuracyPercent: Int,
    onClick: (() -> Unit)?,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(2f)) {
                    Text(title, style = MaterialTheme.typography.bodyMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LexumiOutline)
                    Text("Показів: $timesSeen · Правильних: $totalCorrect", style = MaterialTheme.typography.bodySmall, color = LexumiOutline)
                }
                Text(
                    ratingLabel(rating),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (rating == 4) LexumiSuccess else LexumiOutline,
                )
                Text("$bestStreak", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text("$accuracyPercent%", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = LexumiSuccess)
            }
            // A separate, clearly-tappable button instead of a silently-clickable whole row —
            // easy to miss that a row can be tapped at all otherwise.
            if (onClick != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Повторити вивчення")
                }
            }
        }
    }
}
