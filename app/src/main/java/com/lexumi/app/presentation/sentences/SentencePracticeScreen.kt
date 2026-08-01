@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lexumi.app.presentation.sentences

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.domain.model.AnswerCheck
import com.lexumi.app.presentation.components.BackIconButton
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.LexumiTextField
import com.lexumi.app.presentation.components.PillActionButton
import com.lexumi.app.presentation.components.RuleMultiSelect
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
    val rules by viewModel.rules.collectAsState()
    var answer by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.current) { answer = "" }
    LaunchedEffect(state.current?.id) { state.current?.let { viewModel.speak(it.text) } }
    LaunchedEffect(state.editError) { if (state.editError != null) showEditDialog = true }

    GradientBackground {
        BackIconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(20.dp))

        if (state.current != null) {
            Box(modifier = Modifier.align(Alignment.TopEnd).zIndex(10f).statusBarsPadding().padding(16.dp)) {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.6f), CircleShape),
                ) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Ще")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Редагувати речення") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = { menuExpanded = false; showEditDialog = true },
                    )
                    DropdownMenuItem(
                        text = { Text("Видалити речення") },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        onClick = { menuExpanded = false; showDeleteConfirm = true },
                    )
                }
            }
        }

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
                LexumiTextField(
                    value = answer, onValueChange = { answer = it }, label = "Переклад", singleLine = false,
                    onDone = { viewModel.submit(answer) },
                )
                Spacer(Modifier.height(16.dp))
                PillActionButton(text = "Перевірити", icon = Icons.Filled.Check, onClick = { viewModel.submit(answer) })
                Spacer(Modifier.height(8.dp))
                PillActionButton(
                    text = androidx.compose.ui.res.stringResource(com.lexumi.app.R.string.already_know),
                    icon = Icons.Filled.Check,
                    onClick = { viewModel.markCurrentAsKnown() },
                )
            } else {
                val isCorrect = result.check is AnswerCheck.Correct
                val label = if (isCorrect) "Правильно! ✓" else "Правильна відповідь:"
                Text(label, color = if (isCorrect) LexumiSuccess else LexumiError, style = MaterialTheme.typography.titleMedium)

                if (!isCorrect) {
                    Spacer(Modifier.height(8.dp))
                    val badIndex = result.badWordIndex
                    val userWord = result.userWordAtBadIndex
                    if (badIndex != null && userWord != null) {
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

        if (showEditDialog && state.current != null) {
            EditSentenceDialog(
                initialText = state.current!!.text,
                initialTranslations = state.current!!.translations,
                initialRuleIds = state.current!!.ruleIds.toSet(),
                rules = rules,
                error = state.editError,
                onClearError = { viewModel.clearEditError() },
                onDismiss = { showEditDialog = false; viewModel.clearEditError() },
                onSave = { text, translations, ruleIds ->
                    viewModel.editCurrentSentence(text, translations, ruleIds)
                    showEditDialog = false
                },
            )
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Видалити речення?") },
                text = { Text("Цю дію не можна скасувати.") },
                confirmButton = {
                    TextButton(onClick = { showDeleteConfirm = false; viewModel.deleteCurrentSentence() }) {
                        Text("Видалити", color = LexumiError)
                    }
                },
                dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Скасувати") } },
            )
        }
    }
}

@Composable
private fun EditSentenceDialog(
    initialText: String,
    initialTranslations: List<String>,
    initialRuleIds: Set<Long>,
    rules: List<com.lexumi.app.domain.model.Rule>,
    error: String?,
    onClearError: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, List<String>, List<Long>) -> Unit,
) {
    var text by remember { mutableStateOf(initialText) }
    val translations = remember { mutableStateListOf(*initialTranslations.toTypedArray()).apply { if (isEmpty()) add("") } }
    var ruleIds by remember { mutableStateOf(initialRuleIds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редагувати речення") },
        text = {
            Column {
                if (error != null) {
                    Text(error, color = LexumiError, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }
                LexumiTextField(value = text, onValueChange = { text = it; onClearError() }, label = "Речення", singleLine = false)
                Spacer(Modifier.height(8.dp))
                Text("Варіанти перекладу", style = MaterialTheme.typography.bodyMedium)
                translations.forEachIndexed { index, t ->
                    LexumiTextField(
                        value = t, onValueChange = { translations[index] = it; onClearError() },
                        label = if (index == 0) "Основний переклад" else "Ще один варіант",
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                TextButton(onClick = { translations.add("") }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Додати варіант")
                }
                if (rules.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    RuleMultiSelect(
                        rules = rules,
                        selectedIds = ruleIds,
                        onToggle = { id -> ruleIds = if (id in ruleIds) ruleIds - id else ruleIds + id },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text, translations.toList(), ruleIds.toList()) }) { Text("Зберегти") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
    )
}
