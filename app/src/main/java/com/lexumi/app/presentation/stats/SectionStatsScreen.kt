package com.lexumi.app.presentation.stats

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
import kotlin.math.roundToInt

@Composable
fun SectionStatsScreen(
    onBack: () -> Unit,
    viewModel: SectionStatsViewModel = hiltViewModel(),
) {
    val stats by viewModel.stats.collectAsState()
    val loading by viewModel.loading.collectAsState()

    GradientBackground {
        BackIconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(20.dp))
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp).padding(top = 48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Статистика розділу", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))

            if (loading) {
                CircularProgressIndicator()
            } else if (stats.uniqueWordCount == 0) {
                Text("У цьому розділі ще немає слів.", style = MaterialTheme.typography.bodyMedium)
            } else {
                val percent = (stats.learnedCount * 100.0 / stats.uniqueWordCount).roundToInt()

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.55f)),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${stats.uniqueWordCount}", style = MaterialTheme.typography.displayLarge)
                        Text("унікальних слів у розділі", style = MaterialTheme.typography.bodyMedium)

                        Spacer(Modifier.height(24.dp))

                        Text("${stats.learnedCount} з ${stats.uniqueWordCount}", style = MaterialTheme.typography.headlineMedium, color = LexumiSuccess)
                        Text("вже вивчено", style = MaterialTheme.typography.bodyMedium, color = LexumiOutline)

                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { stats.learnedCount / stats.uniqueWordCount.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = LexumiSuccess,
                            trackColor = Color(0xFFE7E3F5),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("$percent%", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
