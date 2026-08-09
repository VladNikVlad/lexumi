@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.domain.model.AnswerCheck
import com.lexumi.app.domain.usecase.SentenceChecker
import com.lexumi.app.presentation.components.BackIconButton
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.LexumiTextField
import com.lexumi.app.presentation.components.LexumiUnderlineTextField
import com.lexumi.app.presentation.components.PillActionButton
import com.lexumi.app.presentation.components.RuleMultiSelect
import com.lexumi.app.presentation.components.rememberMicGatedAction
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
    val startListening = rememberMicGatedAction(viewModel::startListeningForSentence)

    LaunchedEffect(state.prompt?.sentence?.id, state.prompt?.askOriginalFirst) { answer = "" }
    LaunchedEffect(state.prompt?.speakText) { state.prompt?.let { viewModel.speak(it.speakText) } }
    LaunchedEffect(state.editError) { if (state.editError != null) showEditDialog = true }
    // Voice-only (rating 3) sentences start listening the moment the card appears.
    LaunchedEffect(state.prompt?.sentence?.id, state.prompt?.voiceOnly) {
        val prompt = state.prompt
        if (prompt != null && prompt.voiceOnly && state.result == null && !state.listening) startListening()
    }

    GradientBackground {
        BackIconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(20.dp))

        if (state.prompt != null) {
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
            if (state.done || state.prompt == null) {
                Text("Готово! 🎉", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(24.dp))
                PillActionButton(text = "Готово", icon = Icons.Filled.Check, onClick = onDone)
                return@Column
            }

            val prompt = state.prompt!!
            val sentence = prompt.sentence

            if (state.inMistakeReview) {
                Text("Робота над помилками", style = MaterialTheme.typography.labelLarge, color = LexumiError)
                Spacer(Modifier.height(8.dp))
            }
            LinearProgressIndicator(
                progress = { if (state.total == 0) 0f else state.completed / state.total.toFloat() },
                modifier = Modifier.fillMaxWidth(0.7f).height(6.dp).clip(RoundedCornerShape(3.dp)),
            )
            Spacer(Modifier.height(24.dp))
            if (prompt.audioOnly) {
                Text("Слухай уважно…", style = MaterialTheme.typography.titleMedium, color = LexumiOutline)
                Spacer(Modifier.height(12.dp))
                IconButton(
                    onClick = { viewModel.speak(prompt.speakText) },
                    modifier = Modifier.size(56.dp).background(Color.White.copy(alpha = 0.6f), CircleShape),
                ) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = "Прослухати ще раз", modifier = Modifier.size(28.dp), tint = LexumiOutline)
                }
            } else {
                Text(prompt.displayText.orEmpty(), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                IconButton(
                    onClick = { viewModel.speak(prompt.speakText) },
                    modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.6f), CircleShape),
                ) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = "Прослухати ще раз", tint = LexumiOutline)
                }
            }
            Spacer(Modifier.height(20.dp))

            val hint = state.hint
            val result = state.result

            when {
                hint != null -> HintFillBlanks(
                    hint = hint,
                    onInputChange = viewModel::updateHintInput,
                    onToggleReveal = viewModel::toggleHintReveal,
                    onSubmit = viewModel::submitHints,
                )
                result == null && prompt.voiceOnly -> {
                    // Rating 3 — no more typing, just speak the answer out loud.
                    if (state.listening) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Слухаю…", style = MaterialTheme.typography.bodyMedium, color = LexumiOutline)
                    } else {
                        PillActionButton(text = "Говорити", icon = Icons.Filled.VolumeUp, onClick = startListening)
                    }
                    Spacer(Modifier.height(8.dp))
                    // Always visible while testing — shows exactly what the recognizer is hearing (or why it isn't).
                    Text(state.voiceDebug ?: "…", style = MaterialTheme.typography.bodySmall, color = LexumiOutline)
                    Spacer(Modifier.height(8.dp))
                    PillActionButton(
                        text = androidx.compose.ui.res.stringResource(com.lexumi.app.R.string.already_know),
                        icon = Icons.Filled.Check,
                        onClick = { viewModel.markCurrentAsKnown() },
                    )
                    if (!state.voiceDisabled) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.disableVoiceForSession() }) {
                            Text("Я зараз не можу говорити", color = LexumiOutline, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                result == null -> {
                    LexumiUnderlineTextField(
                        value = answer, onValueChange = { answer = it }, label = "Переклад", singleLine = false,
                        modifier = Modifier.fillMaxWidth(),
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
                }
                else -> {
                    val isCorrect = result.category == SentenceChecker.Category.CORRECT
                    val label = if (isCorrect) "Правильно! ✓" else "Правильна відповідь:"
                    if (prompt.voiceOnly && !state.heard.isNullOrBlank()) {
                        Text("Почув(-ла): «${state.heard}»", style = MaterialTheme.typography.bodyMedium, color = LexumiOutline)
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(label, color = if (isCorrect) LexumiSuccess else LexumiError, style = MaterialTheme.typography.titleMedium)

                    if (!isCorrect) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            buildAnnotatedString {
                                result.correctWords.forEachIndexed { index, word ->
                                    if (index > 0) append(" ")
                                    if (index in result.mismatchedIndices) {
                                        withStyle(androidx.compose.ui.text.SpanStyle(color = LexumiError, fontWeight = FontWeight.Bold)) {
                                            append(word)
                                        }
                                    } else {
                                        append(word)
                                    }
                                }
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
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

        if (showEditDialog && state.prompt != null) {
            EditSentenceDialog(
                initialText = state.prompt!!.sentence.text,
                initialTranslations = state.prompt!!.sentence.translations,
                initialRuleIds = state.prompt!!.sentence.ruleIds.toSet(),
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

/** The "close attempt" fill-in-the-blanks retry: correct words are locked in, the 1-2 wrong ones are inputs. */
@Composable
private fun HintFillBlanks(
    hint: HintState,
    onInputChange: (Int, String) -> Unit,
    onToggleReveal: (Int) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Майже! Заповни пропущені слова:", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            hint.correctWords.forEachIndexed { index, word ->
                if (index in hint.blankIndices) {
                    val isWrong = index in hint.wrongFlash
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(110.dp)) {
                        // Tap the small hint icon if stuck — shows the word so you can type it and keep going.
                        if (index in hint.revealed) {
                            Text(word, style = MaterialTheme.typography.bodySmall, color = LexumiOutline)
                            Spacer(Modifier.height(2.dp))
                        }
                        Row(verticalAlignment = Alignment.Bottom) {
                            LexumiUnderlineTextField(
                                value = hint.inputs[index].orEmpty(),
                                onValueChange = { onInputChange(index, it) },
                                modifier = Modifier.width(84.dp),
                                singleLine = true,
                                isError = isWrong,
                                textStyle = androidx.compose.ui.text.TextStyle(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                                onDone = onSubmit,
                            )
                            IconButton(onClick = { onToggleReveal(index) }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    Icons.Filled.Info,
                                    contentDescription = "Показати слово",
                                    tint = LexumiOutline,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                } else {
                    Text(word, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 14.dp))
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        PillActionButton(text = "Перевірити", icon = Icons.Filled.Check, onClick = onSubmit)
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
