package com.lexumi.app.presentation.imagequiz

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.PillActionButton
import com.lexumi.app.presentation.theme.LexumiError
import com.lexumi.app.presentation.theme.LexumiSuccess

@Composable
fun ImageQuizScreen(
    onDone: () -> Unit,
    viewModel: ImageQuizViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    GradientBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.loading) { CircularProgressIndicator(); return@Column }
            if (state.done || state.prompt == null) {
                Text("Тест завершено! 🎉", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(24.dp))
                PillActionButton(text = "Готово", icon = Icons.Filled.Check, onClick = onDone)
                return@Column
            }

            val prompt = state.prompt!!
            LinearProgressIndicator(
                progress = { if (state.total == 0) 0f else state.completed / state.total.toFloat() },
                modifier = Modifier.fillMaxWidth(0.7f).height(6.dp).clip(RoundedCornerShape(3.dp)),
            )
            Spacer(Modifier.height(20.dp))

            Image(
                painter = rememberAsyncImagePainter(prompt.image.imagePath),
                contentDescription = null,
                modifier = Modifier.size(180.dp).clip(RoundedCornerShape(20.dp)),
            )
            Spacer(Modifier.height(24.dp))

            if (state.feedbackCorrect == null) {
                prompt.options.forEach { option ->
                    PillActionButton(text = option, icon = Icons.Filled.Check, onClick = { viewModel.submit(option) }, modifier = Modifier.padding(bottom = 10.dp))
                }
            } else {
                val correct = state.feedbackCorrect == true
                Text(
                    if (correct) "Правильно! ✓" else "Неправильно. Це: ${prompt.image.translation}",
                    color = if (correct) LexumiSuccess else LexumiError,
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(20.dp))
                PillActionButton(text = "Далі", icon = Icons.Filled.Check, onClick = { viewModel.next() })
            }
        }
    }
}
