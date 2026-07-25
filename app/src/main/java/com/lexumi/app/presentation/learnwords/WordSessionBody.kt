package com.lexumi.app.presentation.learnwords

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.LexumiTextField
import com.lexumi.app.presentation.components.PillActionButton
import com.lexumi.app.presentation.theme.LexumiError
import com.lexumi.app.presentation.theme.LexumiSuccess

/**
 * Shared visual body for a word-learning session, used by both the topic
 * "Вчити слова" flow and the "Повторити слова" review flow (point 20 & 26).
 */
@Composable
fun WordSessionBody(
    state: LearnWordsUiState,
    onDone: () -> Unit,
    onSubmitChoice: (String) -> Unit,
    onSubmitTyped: (String) -> Unit,
    onAddToReview: (() -> Unit)?,
    onNext: () -> Unit,
    doneLabel: String = "Готово",
) {
    var typedAnswer by remember { mutableStateOf("") }
    LaunchedEffect(state.prompt) { typedAnswer = "" }

    GradientBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.loading) {
                CircularProgressIndicator()
                return@Column
            }
            if (state.sessionDone || state.prompt == null) {
                Text("Готово! 🎉", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(24.dp))
                PillActionButton(text = doneLabel, icon = Icons.Filled.Check, onClick = onDone)
                return@Column
            }

            val prompt = state.prompt
            LinearProgressIndicator(
                progress = { if (state.totalCount == 0) 0f else state.completedCount / state.totalCount.toFloat() },
                modifier = Modifier.fillMaxWidth(0.7f).height(6.dp).clip(RoundedCornerShape(3.dp)),
            )
            Spacer(Modifier.height(24.dp))

            if (!prompt.askTermFirst && prompt.word.imagePath != null) {
                Image(
                    painter = rememberAsyncImagePainter(prompt.word.imagePath),
                    contentDescription = null,
                    modifier = Modifier.size(120.dp).clip(RoundedCornerShape(16.dp)),
                )
                Spacer(Modifier.height(16.dp))
            }

            Text(
                text = if (prompt.askTermFirst) prompt.word.term else prompt.word.translation,
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(28.dp))

            when (val feedback = state.feedback) {
                WordFeedback.None -> {
                    if (prompt.choices != null) {
                        prompt.choices.forEach { option ->
                            PillActionButton(
                                text = option,
                                icon = Icons.Filled.Check,
                                onClick = { onSubmitChoice(option) },
                                modifier = Modifier.padding(bottom = 10.dp),
                            )
                        }
                    } else {
                        LexumiTextField(value = typedAnswer, onValueChange = { typedAnswer = it }, label = "Ваша відповідь")
                        Spacer(Modifier.height(16.dp))
                        PillActionButton(text = "Перевірити", icon = Icons.Filled.Check, onClick = { onSubmitTyped(typedAnswer) })
                    }
                    if (onAddToReview != null) {
                        TextButton(onClick = onAddToReview) {
                            Icon(Icons.Filled.BookmarkAdd, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Додати до списку повторення")
                        }
                    }
                }
                is WordFeedback.Correct -> {
                    Text("Правильно! ✓", color = LexumiSuccess, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(20.dp))
                    PillActionButton(text = "Далі", icon = Icons.Filled.Check, onClick = onNext)
                }
                is WordFeedback.OneLetterTypo -> {
                    Text("Майже! Правильно: ${feedback.correctSpelling}", color = LexumiSuccess, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(20.dp))
                    PillActionButton(text = "Далі", icon = Icons.Filled.Check, onClick = onNext)
                }
                is WordFeedback.Wrong -> {
                    Text("Неправильно. Правильно: ${feedback.correctSpelling}", color = LexumiError, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(20.dp))
                    PillActionButton(text = "Далі", icon = Icons.Filled.Check, onClick = onNext)
                }
            }
        }
    }
}
