package com.lexumi.app.presentation.learnwords

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.lexumi.app.domain.model.Rule
import com.lexumi.app.domain.usecase.TranslationParser
import com.lexumi.app.presentation.components.BackIconButton
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.LexumiTextField
import com.lexumi.app.presentation.components.PillActionButton
import com.lexumi.app.presentation.theme.LexumiError
import com.lexumi.app.presentation.theme.LexumiSuccess
import com.lexumi.app.util.ImageCompressor
import java.io.File

/**
 * Shared visual body for a word-learning session, used by both the topic
 * "Вчити слова" flow and the "Повторити слова" review flow (point 20 & 26).
 * The three-dot menu (top-right) lets the user edit or delete the word
 * currently on screen — image included — when [onEditWord]/[onDeleteWord]
 * are provided.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordSessionBody(
    state: LearnWordsUiState,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onSubmitChoice: (String) -> Unit,
    onSubmitTyped: (String) -> Unit,
    onAddToReview: (() -> Unit)?,
    onAlreadyKnow: (() -> Unit)? = null,
    onNext: () -> Unit,
    onSelectMatchingLeft: ((Long) -> Unit)? = null,
    onSelectMatchingRight: ((Long) -> Unit)? = null,
    doneLabel: String = "Готово",
    availableRules: List<Rule> = emptyList(),
    onEditWord: ((term: String, translation: String, imagePath: String?, ruleId: Long?) -> Unit)? = null,
    onDeleteWord: (() -> Unit)? = null,
    onClearEditError: () -> Unit = {},
    onSpeak: ((String) -> Unit)? = null,
) {
    var typedAnswer by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAnswerHint by remember { mutableStateOf(false) }
    LaunchedEffect(state.prompt) { typedAnswer = ""; showAnswerHint = false }
    LaunchedEffect(state.editError) { if (state.editError != null) showEditDialog = true }

    // Read the word aloud automatically the moment it appears (cleaned to a single
    // variant — the raw field may contain "/" alternatives and a "(...)" hint).
    LaunchedEffect(state.prompt?.word?.id, state.prompt?.askTermFirst) {
        val prompt = state.prompt ?: return@LaunchedEffect
        val raw = if (prompt.askTermFirst) prompt.word.term else prompt.word.translation
        onSpeak?.invoke(TranslationParser.displayPrimary(raw))
    }

    GradientBackground {
        BackIconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(16.dp))
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.loading) {
                CircularProgressIndicator()
                return@Column
            }
            if (state.matchingGame != null) {
                MatchingGameBody(
                    game = state.matchingGame,
                    onSelectLeft = onSelectMatchingLeft ?: {},
                    onSelectRight = onSelectMatchingRight ?: {},
                    onSpeak = onSpeak,
                )
                return@Column
            }
            if (state.sessionDone || state.prompt == null) {
                Text("Готово! 🎉", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(24.dp))
                PillActionButton(text = doneLabel, icon = Icons.Filled.Check, onClick = onDone)
                return@Column
            }

            val prompt = state.prompt
            if (state.inMistakeReview) {
                Text(
                    "Робота над помилками",
                    style = MaterialTheme.typography.labelLarge,
                    color = com.lexumi.app.presentation.theme.LexumiError,
                )
                Spacer(Modifier.height(8.dp))
            }
            LinearProgressIndicator(
                progress = { if (state.totalCount == 0) 0f else state.completedCount / state.totalCount.toFloat() },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = Color(0xFFB8AFD9),
                trackColor = Color(0xFFE7E3F5),
            )
            Spacer(Modifier.height(28.dp))

            if (!prompt.askTermFirst && prompt.word.imagePath != null) {
                Image(
                    painter = rememberAsyncImagePainter(prompt.word.imagePath),
                    contentDescription = null,
                    modifier = Modifier.size(120.dp).clip(RoundedCornerShape(16.dp)),
                )
                Spacer(Modifier.height(16.dp))
            }

            val promptRaw = if (prompt.askTermFirst) prompt.word.term else prompt.word.translation
            val promptClean = TranslationParser.displayPrimary(promptRaw)
            // Typed mode only: if the user is stuck, tapping the word reveals the
            // answer above it so they can type it in and keep moving instead of
            // getting stuck on a word they don't remember yet.
            val hintAllowed = state.feedback == WordFeedback.None && prompt.choices == null
            val answerRaw = if (prompt.askTermFirst) prompt.word.translation else prompt.word.term
            if (hintAllowed && showAnswerHint) {
                Text(
                    answerRaw.trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = com.lexumi.app.presentation.theme.LexumiOutline,
                )
                Spacer(Modifier.height(8.dp))
            }
            WordDisplayCard(
                text = promptClean,
                subtext = if (TranslationParser.hasExtra(promptRaw)) promptRaw.trim() else null,
                onSpeak = onSpeak?.let { speak -> { speak(promptClean) } },
                onTap = if (hintAllowed) ({ showAnswerHint = !showAnswerHint }) else null,
            )
            Spacer(Modifier.height(28.dp))

            when (val feedback = state.feedback) {
                WordFeedback.None -> {
                    if (prompt.choices != null) {
                        prompt.choices.forEach { option ->
                            AnswerOptionButton(
                                text = option,
                                onClick = { onSubmitChoice(option) },
                                modifier = Modifier.padding(bottom = 12.dp),
                            )
                        }
                    } else {
                        LexumiTextField(
                            value = typedAnswer, onValueChange = { typedAnswer = it }, label = "Ваша відповідь",
                            onDone = { onSubmitTyped(typedAnswer) },
                        )
                        Spacer(Modifier.height(16.dp))
                        PillActionButton(text = "Перевірити", icon = Icons.Filled.Check, onClick = { onSubmitTyped(typedAnswer) })
                    }

                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = Color(0xFFE7E3F5))
                    Spacer(Modifier.height(12.dp))

                    if (onAlreadyKnow != null) {
                        TextButton(onClick = onAlreadyKnow) {
                            Text(
                                androidx.compose.ui.res.stringResource(com.lexumi.app.R.string.already_know) + " →",
                                color = com.lexumi.app.presentation.theme.LexumiOutline,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (onAddToReview != null) {
                        PillActionButton(
                            text = "Додати до повторення",
                            icon = Icons.Filled.BookmarkAdd,
                            onClick = onAddToReview,
                        )
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

        // Top-right "⋮" menu — edit or delete the word currently on screen.
        if ((onEditWord != null || onDeleteWord != null) && state.prompt != null) {
            Box(modifier = Modifier.align(Alignment.TopEnd).zIndex(10f).statusBarsPadding().padding(16.dp)) {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.6f), CircleShape),
                ) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Ще")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    if (onEditWord != null) {
                        DropdownMenuItem(
                            text = { Text("Редагувати слово") },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                            onClick = { menuExpanded = false; showEditDialog = true },
                        )
                    }
                    if (onDeleteWord != null) {
                        DropdownMenuItem(
                            text = { Text("Видалити слово") },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                            onClick = { menuExpanded = false; showDeleteConfirm = true },
                        )
                    }
                }
            }
        }

        if (showEditDialog && state.prompt != null && onEditWord != null) {
            EditWordDialog(
                initialTerm = state.prompt.word.term,
                initialTranslation = state.prompt.word.translation,
                initialImagePath = state.prompt.word.imagePath,
                initialRuleId = state.prompt.word.ruleId,
                timesSeen = state.prompt.word.timesSeen,
                totalCorrect = state.prompt.word.totalCorrect,
                bestStreak = state.prompt.word.bestStreak,
                rules = availableRules,
                error = state.editError,
                onClearError = onClearEditError,
                onDismiss = { showEditDialog = false; onClearEditError() },
                onSave = { term, translation, imagePath, ruleId ->
                    onEditWord(term, translation, imagePath, ruleId)
                    showEditDialog = false
                },
            )
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Видалити слово?") },
                text = { Text("Цю дію не можна скасувати.") },
                confirmButton = {
                    TextButton(onClick = { showDeleteConfirm = false; onDeleteWord?.invoke() }) {
                        Text("Видалити", color = LexumiError)
                    }
                },
                dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Скасувати") } },
            )
        }
    }
}

@Composable
private fun ColumnScope.MatchingGameBody(
    game: MatchingGameState,
    onSelectLeft: (Long) -> Unit,
    onSelectRight: (Long) -> Unit,
    onSpeak: ((String) -> Unit)?,
) {
    Text("Знайти пару", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(8.dp))
    Text(
        "${game.matchedIds.size} з ${game.pairs.size}",
        style = MaterialTheme.typography.bodyMedium,
        color = com.lexumi.app.presentation.theme.LexumiOutline,
    )
    Spacer(Modifier.height(20.dp))

    val pairsById = remember(game.pairs) { game.pairs.associateBy { it.wordId } }
    // Matched pairs disappear from the board entirely instead of just being greyed out.
    val activeLeft = game.leftOrder.filterNot { it in game.matchedIds }
    val activeRight = game.rightOrder.filterNot { it in game.matchedIds }

    // weight(1f) bounds the height to whatever room is left in the parent
    // Column, so each side's LazyColumn can scroll instead of overflowing
    // the screen when there are more than ~10 pairs in the session.
    Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(activeLeft, key = { it }) { id ->
                MatchingTile(
                    text = TranslationParser.displayPrimary(pairsById.getValue(id).term),
                    matched = false,
                    selected = id == game.selectedLeftId,
                    wrongFlash = id == game.wrongFlashLeftId,
                    onClick = { onSpeak?.invoke(TranslationParser.displayPrimary(pairsById.getValue(id).term)); onSelectLeft(id) },
                )
            }
        }
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(activeRight, key = { it }) { id ->
                MatchingTile(
                    text = TranslationParser.displayPrimary(pairsById.getValue(id).translation),
                    matched = false,
                    selected = id == game.selectedRightId,
                    wrongFlash = id == game.wrongFlashRightId,
                    onClick = { onSpeak?.invoke(TranslationParser.displayPrimary(pairsById.getValue(id).term)); onSelectRight(id) },
                )
            }
        }
    }
}

@Composable
private fun MatchingTile(text: String, matched: Boolean, selected: Boolean, wrongFlash: Boolean, onClick: () -> Unit) {
    val background = when {
        matched -> com.lexumi.app.presentation.theme.LexumiSuccess.copy(alpha = 0.35f)
        wrongFlash -> LexumiError.copy(alpha = 0.35f)
        selected -> com.lexumi.app.presentation.theme.LexumiOutline.copy(alpha = 0.35f)
        else -> Color.White.copy(alpha = 0.75f)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .then(if (matched) Modifier else Modifier.clickable(onClick = onClick))
            .padding(vertical = 14.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun WordDisplayCard(text: String, subtext: String? = null, onSpeak: (() -> Unit)?, onTap: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.55f))
            .then(if (onTap != null) Modifier.clickable(onClick = onTap) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        // Soft decorative blob peeking from the corner, matching the app's background style.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 24.dp, y = 24.dp)
                .size(90.dp)
                .clip(CircleShape)
                .background(com.lexumi.app.presentation.theme.LexumiTealLight.copy(alpha = 0.5f)),
        )
        Column(
            modifier = Modifier.padding(vertical = 36.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = text, style = MaterialTheme.typography.headlineLarge)
            if (subtext != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.bodySmall,
                    color = com.lexumi.app.presentation.theme.LexumiOutline,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            if (onSpeak != null) {
                Spacer(Modifier.height(16.dp))
                IconButton(
                    onClick = onSpeak,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.7f)),
                ) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = "Прослухати ще раз", tint = com.lexumi.app.presentation.theme.LexumiOutline)
                }
            }
        }
    }
}

@Composable
private fun AnswerOptionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.75f))
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditWordDialog(
    initialTerm: String,
    initialTranslation: String,
    initialImagePath: String?,
    initialRuleId: Long?,
    timesSeen: Int,
    totalCorrect: Int,
    bestStreak: Int,
    rules: List<Rule>,
    error: String?,
    onClearError: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String, String?, Long?) -> Unit,
) {
    val context = LocalContext.current
    var term by remember { mutableStateOf(initialTerm) }
    var translation by remember { mutableStateOf(initialTranslation) }
    var imagePath by remember { mutableStateOf(initialImagePath) }
    var ruleId by remember { mutableStateOf(initialRuleId) }
    var ruleMenuExpanded by remember { mutableStateOf(false) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val file = File(context.filesDir, "word_${System.currentTimeMillis()}.jpg")
        val result = ImageCompressor.compressToFile(context, uri, file, maxBytes = 100 * 1024)
        if (result != null) imagePath = result.absolutePath
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редагувати слово") },
        text = {
            Column {
                Text(
                    "Показів: $timesSeen · Правильних: $totalCorrect · Найдовша серія: $bestStreak",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(10.dp))
                if (error != null) {
                    Text(error, color = LexumiError, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.5f))
                        .clickable { pickImage.launch("image/*") },
                    contentAlignment = Alignment.Center,
                ) {
                    if (imagePath != null) {
                        Image(painter = rememberAsyncImagePainter(imagePath), contentDescription = null, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(imageVector = Icons.Filled.ImageIcon, contentDescription = "Додати фото")
                    }
                }
                Spacer(Modifier.height(12.dp))
                LexumiTextField(value = term, onValueChange = { term = it; onClearError() }, label = "Слово")
                Spacer(Modifier.height(8.dp))
                LexumiTextField(value = translation, onValueChange = { translation = it; onClearError() }, label = "Переклад")
                if (rules.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    ExposedDropdownMenuBox(expanded = ruleMenuExpanded, onExpandedChange = { ruleMenuExpanded = it }) {
                        LexumiTextField(
                            value = rules.firstOrNull { it.id == ruleId }?.name ?: "Без правила",
                            onValueChange = {},
                            label = "Правило",
                            modifier = Modifier.menuAnchor(),
                        )
                        ExposedDropdownMenu(expanded = ruleMenuExpanded, onDismissRequest = { ruleMenuExpanded = false }) {
                            DropdownMenuItem(text = { Text("Без правила") }, onClick = { ruleId = null; ruleMenuExpanded = false })
                            rules.forEach { r ->
                                DropdownMenuItem(text = { Text(r.name) }, onClick = { ruleId = r.id; ruleMenuExpanded = false })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(term, translation, imagePath, ruleId) }) { Text("Зберегти") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
    )
}
