package com.lexumi.app.presentation.sentences

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.domain.model.AnswerCheck
import com.lexumi.app.presentation.components.BackIconButton
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.LexumiTextField
import com.lexumi.app.presentation.components.PillActionButton
import com.lexumi.app.presentation.components.highlightWordDiff
import com.lexumi.app.presentation.theme.LexumiError
import com.lexumi.app.presentation.theme.LexumiOutline
import com.lexumi.app.presentation.theme.LexumiSuccess

@Composable
fun SentencePracticeScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: SentencePracticeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var answer by remember { mutableStateOf("") }
    LaunchedEffect(state.current) { answer = "" }
    LaunchedEffect(state.current?.id) { state.current?.let { viewModel.speak(it.text) } }

    GradientBackground {
        BackIconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(20.dp))
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
            Spacer(Modifier.height(8.dp))
            IconButton(
                onClick = { viewModel.speak(sentence.text) },
                modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.6f), CircleShape),
            ) {
                Icon(Icons.Filled.VolumeUp, contentDescription = "Прослухати ще раз", tint = LexumiOutline)
            }
            Spacer(Modifier.height(20.dp))

            val result = state.result
            if (result == null) {
                LexumiTextField(value = answer, onValueChange = { answer = it }, label = "Переклад", singleLine = false)
                Spacer(Modifier.height(16.dp))
                PillActionButton(text = "Перевірити", icon = Icons.Filled.Check, onClick = { viewModel.submit(answer) })
            } else {
                val isCorrect = result.check is AnswerCheck.Correct
                val label = if (isCorrect) "Правильно! ✓" else "Правильна відповідь:"
                Text(label, color = if (isCorrect) LexumiSuccess else LexumiError, style = MaterialTheme.typography.titleMedium)

                if (!isCorrect) {
                    Spacer(Modifier.height(8.dp))
                    val badIndex = result.badWordIndex
                    val userWord = result.userWordAtBadIndex
                    if (badIndex != null && userWord != null) {
                        // Show the correct sentence with just the wrong word/letter highlighted red.
                        val before = result.correctWords.subList(0, badIndex).joinToString(" ")
                        val after = result.correctWords.subList(badIndex + 1, result.correctWords.size).joinToString(" ")
                        val highlightedWord = highlightWordDiff(userWord, result.correctWords[badIndex])
                        Text(
                            buildAnnotatedString {
                                if (before.isNotEmpty()) append("$before ")
                                append(highlightedWord)
                                if (after.isNotEmpty()) append(" $after")
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                    } else {
                        Text(result.correctWords.joinToString(" "), style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    "Показів: ${sentence.timesSeen} · Правильних: ${sentence.totalCorrect} · Найдовша серія: ${sentence.bestStreak}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(20.dp))
                PillActionButton(text = "Далі", icon = Icons.Filled.Check, onClick = { viewModel.next() })
            }
        }
    }
}
