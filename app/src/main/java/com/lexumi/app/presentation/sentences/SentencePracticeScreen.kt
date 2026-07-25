package com.lexumi.app.presentation.sentences

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.domain.model.AnswerCheck
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.LexumiTextField
import com.lexumi.app.presentation.components.PillActionButton
import com.lexumi.app.presentation.theme.LexumiError
import com.lexumi.app.presentation.theme.LexumiSuccess

@Composable
fun SentencePracticeScreen(
    onDone: () -> Unit,
    viewModel: SentencePracticeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var answer by remember { mutableStateOf("") }
    LaunchedEffect(state.current) { answer = "" }

    GradientBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.loading) { CircularProgressIndicator(); return@Column }
            if (state.done || state.current == null) {
                Text("Готово! 🎉", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(24.dp))
                PillActionButton(text = "Готово", icon = Icons.Filled.Check, onClick = onDone)
                return@Column
            }

            val sentence = state.current!!
            LinearProgressIndicator(
                progress = { if (state.total == 0) 0f else state.completed / state.total.toFloat() },
                modifier = Modifier.fillMaxWidth(0.7f).height(6.dp).clip(RoundedCornerShape(3.dp)),
            )
            Spacer(Modifier.height(24.dp))
            Text(sentence.text, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(20.dp))

            if (state.feedback == null) {
                LexumiTextField(value = answer, onValueChange = { answer = it }, label = "Переклад", singleLine = false)
                Spacer(Modifier.height(16.dp))
                PillActionButton(text = "Перевірити", icon = Icons.Filled.Check, onClick = { viewModel.submit(answer) })
            } else {
                val fb = state.feedback!!
                val (label, color) = when (fb) {
                    is AnswerCheck.Correct -> "Правильно! ✓" to LexumiSuccess
                    is AnswerCheck.OneLetterTypo -> "Майже! Правильно: ${fb.correctSpelling}" to LexumiSuccess
                    is AnswerCheck.Wrong -> "Неправильно. Правильно: ${fb.correctSpelling}" to LexumiError
                }
                Text(label, color = color, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(20.dp))
                PillActionButton(text = "Далі", icon = Icons.Filled.Check, onClick = { viewModel.next() })
            }
        }
    }
}
