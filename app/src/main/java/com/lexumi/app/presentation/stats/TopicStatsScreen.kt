package com.lexumi.app.presentation.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.lexumi.app.presentation.theme.LexumiOutline
import com.lexumi.app.presentation.theme.LexumiSuccess

@Composable
fun TopicStatsScreen(
    onBack: () -> Unit,
    viewModel: TopicStatsViewModel = hiltViewModel(),
) {
    val rows by viewModel.rows.collectAsState()

    GradientBackground {
        BackIconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(20.dp))
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp).padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Статистика слів", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            if (rows.isEmpty()) {
                Text("Ще немає даних — почни вчити слова цієї теми.", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                Text("Слово", modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelLarge)
                Text("Повторів", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                Text("Правильно", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                Text("Серія", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                Text("%", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            }

            rows.forEach { row ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f)),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(2f)) {
                            Text(row.term, style = MaterialTheme.typography.bodyMedium)
                            Text(row.translation, style = MaterialTheme.typography.bodySmall, color = LexumiOutline)
                        }
                        Text("${row.timesSeen}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text("${row.totalCorrect}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text("${row.bestStreak}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${row.accuracyPercent}%",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LexumiSuccess,
                        )
                    }
                }
            }
        }
    }
}
