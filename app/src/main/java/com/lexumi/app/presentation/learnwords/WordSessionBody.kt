package com.lexumi.app.presentation.learnwords

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.lexumi.app.presentation.theme.LexumiIndigo
import com.lexumi.app.presentation.theme.LexumiOutline
import com.lexumi.app.presentation.theme.LexumiPurpleLight
import com.lexumi.app.presentation.theme.LexumiSuccess
import com.lexumi.app.util.ImageCompressor
import java.io.File

/**
 * Shared visual body for a word-learning session, used by both the topic
 * "Вчити слова" flow and the "Повторити слова" review flow. The three-dot
 * menu (top-right) lets the user edit or delete the word currently on
 * screen — image included — when [onEditWord]/[onDeleteWord] are provided.
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
    onStartVoiceCard: (() -> Unit)? = null,
    onNextVoiceCard: (() -> Unit)? = null,
    onMarkVoiceCardKnown: (() -> Unit)? = null,
    onVoiceUnavailable: (() -> Unit)? = null,
    onSelectMatchingLeft: ((Long) -> Unit)? = null,
    onSelectMatchingRight: ((Long) -> Unit)? = null,
    onStartHearOnlyVoice: (() -> Unit)? = null,
    onDisableVoiceForSession: (() -> Unit)? = null,
    onEditVoiceCardWord: ((term: String, translation: String, imagePath: String?, ruleId: Long?) -> Unit)? = null,
    onDeleteVoiceCardWord: (() -> Unit)? = null,
    doneLabel: String = "Готово",
    availableRules: List<Rule> = emptyList(),
    onEditWord: ((term: String, translation: String, imagePath: String?, ruleId: Long?) -> Unit)? = null,
    onDeleteWord: (() -> Unit)? = null,
    onClearEditError: () -> Unit = {},
    onSpeak: ((String) -> Unit)? = null,
    onSpeakNative: ((String) -> Unit)? = null,
) {
    var typedAnswer by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAnswerHint by remember { mutableStateOf(false) }
    LaunchedEffect(state.prompt) { typedAnswer = ""; showAnswerHint = false }
    LaunchedEffect(state.editError) { if (state.editError != null) showEditDialog = true }

    // Read the word/prompt aloud automatically the moment it appears.
    LaunchedEffect(state.prompt?.word?.id, state.prompt?.mode) {
        val prompt = state.prompt ?: return@LaunchedEffect
        onSpeak?.invoke(TranslationParser.displayPrimary(prompt.speakText))
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
            if (state.voiceMastery != null) {
                VoiceMasteryBody(
                    state = state.voiceMastery,
                    onStartListening = onStartVoiceCard ?: {},
                    onNext = onNextVoiceCard ?: {},
                    onMarkKnown = onMarkVoiceCardKnown,
                    onVoiceUnavailable = onVoiceUnavailable,
                )
                return@Column
            }
            if (state.matchingGame != null) {
                MatchingGameBody(
                    game = state.matchingGame,
                    onSelectLeft = onSelectMatchingLeft ?: {},
                    onSelectRight = onSelectMatchingRight ?: {},
                    onSpeakTerm = onSpeak,
                    onSpeakTranslation = onSpeakNative ?: onSpeak,
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
                Text("Робота над помилками", style = MaterialTheme.typography.labelLarge, color = LexumiError)
                Spacer(Modifier.height(8.dp))
            }
            LinearProgressIndicator(
                progress = { if (state.totalCount == 0) 0f else state.completedCount / state.totalCount.toFloat() },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = Color(0xFFB8AFD9),
                trackColor = Color(0xFFE7E3F5),
            )
            Spacer(Modifier.height(28.dp))

            val hintAllowed = state.feedback == WordFeedback.None && prompt.choices == null && prompt.mode != WordPromptMode.HEAR_ONLY
            if (hintAllowed && showAnswerHint) {
                Text(prompt.expectedAnswer.trim(), style = MaterialTheme.typography.bodyMedium, color = LexumiOutline)
                Spacer(Modifier.height(8.dp))
            }

            if (prompt.mode == WordPromptMode.HEAR_ONLY) {
                // Nothing is shown — only heard. A big replay button stands in for the word card.
                HearOnlyCard(onReplay = onSpeak?.let { speak -> { speak(TranslationParser.displayPrimary(prompt.speakText)) } })
                if (state.voiceDebug != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(state.voiceDebug, style = MaterialTheme.typography.bodySmall, color = LexumiOutline)
                }
            } else {
                val promptClean = TranslationParser.displayPrimary(prompt.displayText.orEmpty())
                WordDisplayCard(
                    text = promptClean,
                    subtext = if (TranslationParser.hasExtra(prompt.displayText.orEmpty())) prompt.displayText?.trim() else null,
                    onSpeak = onSpeak?.let { speak -> { speak(promptClean) } },
                    onTap = if (hintAllowed) ({ showAnswerHint = !showAnswerHint }) else null,
                )
            }
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
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            PillActionButton(
                                text = "Перевірити", icon = Icons.Filled.Check,
                                onClick = { onSubmitTyped(typedAnswer) },
                                modifier = Modifier.weight(1f),
                            )
                            if (prompt.mode == WordPromptMode.HEAR_ONLY && !state.voiceDisabled && onStartHearOnlyVoice != null) {
                                IconButton(
                                    onClick = onStartHearOnlyVoice,
                                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)),
                                ) { Icon(Icons.Filled.Mic, contentDescription = "Сказати вголос") }
                            }
                        }
                        if (prompt.mode == WordPromptMode.HEAR_ONLY && !state.voiceDisabled && onDisableVoiceForSession != null) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = onDisableVoiceForSession) {
                                Text("Я зараз не можу говорити", color = LexumiOutline, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = Color(0xFFE7E3F5))
                    Spacer(Modifier.height(12.dp))

                    if (onAlreadyKnow != null) {
                        TextButton(onClick = onAlreadyKnow) {
                            Text(
                                androidx.compose.ui.res.stringResource(com.lexumi.app.R.string.already_know) + " →",
                                color = LexumiOutline,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (onAddToReview != null) {
                        PillActionButton(text = "Додати до повторення", icon = Icons.Filled.BookmarkAdd, onClick = onAddToReview)
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

        // Top-right "⋮" menu — edit or delete whichever word is currently on screen, be it the
        // main-pass prompt or the current "say it aloud" cards-round card.
        val menuWord = state.prompt?.word ?: state.voiceMastery?.let { it.cards.getOrNull(it.index)?.word }
        val onEditCurrentWord = if (state.prompt != null) onEditWord else onEditVoiceCardWord
        val onDeleteCurrentWord = if (state.prompt != null) onDeleteWord else onDeleteVoiceCardWord
        if ((onEditCurrentWord != null || onDeleteCurrentWord != null) && menuWord != null) {
            Box(modifier = Modifier.align(Alignment.TopEnd).zIndex(10f).statusBarsPadding().padding(16.dp)) {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.6f), CircleShape),
                ) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Ще")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    if (onEditCurrentWord != null) {
                        DropdownMenuItem(
                            text = { Text("Редагувати слово") },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                            onClick = { menuExpanded = false; showEditDialog = true },
                        )
                    }
                    if (onDeleteCurrentWord != null) {
                        DropdownMenuItem(
                            text = { Text("Видалити слово") },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                            onClick = { menuExpanded = false; showDeleteConfirm = true },
                        )
                    }
                }
            }
        }

        if (showEditDialog && menuWord != null && onEditCurrentWord != null) {
            EditWordDialog(
                initialTerm = menuWord.term,
                initialTranslation = menuWord.translation,
                initialImagePath = menuWord.imagePath,
                initialRuleId = menuWord.ruleId,
                timesSeen = menuWord.timesSeen,
                totalCorrect = menuWord.totalCorrect,
                bestStreak = menuWord.bestStreak,
                rules = availableRules,
                error = state.editError,
                onClearError = onClearEditError,
                onDismiss = { showEditDialog = false; onClearEditError() },
                onSave = { term, translation, imagePath, ruleId ->
                    onEditCurrentWord(term, translation, imagePath, ruleId)
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
                    TextButton(onClick = { showDeleteConfirm = false; onDeleteCurrentWord?.invoke() }) {
                        Text("Видалити", color = LexumiError)
                    }
                },
                dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Скасувати") } },
            )
        }
    }
}

/** Rating-3 "hear only" prompt: nothing shown, just a big replay button. */
@Composable
private fun HearOnlyCard(onReplay: (() -> Unit)?) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color.White.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(modifier = Modifier.padding(vertical = 44.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Слухай уважно…", style = MaterialTheme.typography.titleMedium, color = LexumiOutline)
            Spacer(Modifier.height(16.dp))
            IconButton(
                onClick = { onReplay?.invoke() },
                modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)),
            ) {
                Icon(Icons.Filled.VolumeUp, contentDescription = "Прослухати ще раз", modifier = Modifier.size(32.dp), tint = LexumiOutline)
            }
        }
    }
}

/**
 * The "say it out loud" mic button: a soft lavender circle inside a thin ring, with an outline
 * mic icon — the main call-to-action for the cards round, replacing the old text pill. While
 * [listening] it gently pulses so it's obvious the app is actively recording.
 */
@Composable
private fun MicCircleButton(onClick: () -> Unit, listening: Boolean = false, size: androidx.compose.ui.unit.Dp = 132.dp, iconSize: androidx.compose.ui.unit.Dp = 48.dp) {
    val pulseTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "mic-pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (listening) 1.08f else 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(650, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "mic-pulse-scale",
    )
    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
            .size(size)
            .clip(CircleShape)
            .then(
                if (listening) Modifier.background(LexumiPurpleLight.copy(alpha = 0.25f))
                else Modifier,
            )
            .border(1.5.dp, LexumiOutline.copy(alpha = if (listening) 0.7f else 0.35f), CircleShape)
            .padding(9.dp)
            .clip(CircleShape)
            .background(LexumiPurpleLight.copy(alpha = if (listening) 0.75f else 0.55f))
            .then(if (listening) Modifier else Modifier.clickable(onClick = onClick)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Mic,
            contentDescription = "Говорити",
            tint = LexumiIndigo,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun ColumnScope.VoiceMasteryBody(
    state: VoiceMasteryState,
    onStartListening: () -> Unit,
    onNext: () -> Unit,
    onMarkKnown: (() -> Unit)?,
    onVoiceUnavailable: (() -> Unit)?,
) {
    val card = state.cards.getOrNull(state.index) ?: return
    Text("Скажи це слово вголос", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(8.dp))
    Text("${state.index + 1} з ${state.cards.size}", style = MaterialTheme.typography.bodyMedium, color = LexumiOutline)
    Spacer(Modifier.height(28.dp))

    // Listening no longer starts on its own the moment a card appears — the user needs a moment
    // to read the word and recall the translation first, then tap the mic when ready to say it.
    WordDisplayCard(text = TranslationParser.displayPrimary(card.word.translation), onSpeak = null)
    Spacer(Modifier.height(12.dp))
    // Always visible while testing — shows exactly what the recognizer is hearing (or why it isn't).
    Text(
        state.debug ?: "…",
        style = MaterialTheme.typography.bodySmall,
        color = LexumiOutline,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
    Spacer(Modifier.height(16.dp))

    when {
        state.listening -> {
            MicCircleButton(onClick = {}, listening = true)
            Spacer(Modifier.height(12.dp))
            Text("Слухаю…", style = MaterialTheme.typography.bodyMedium, color = LexumiOutline)
        }
        state.correct == true -> {
            Icon(Icons.Filled.Check, contentDescription = null, tint = LexumiSuccess, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(8.dp))
            Text("Правильно!", style = MaterialTheme.typography.titleMedium, color = LexumiSuccess)
            Spacer(Modifier.height(20.dp))
            PillActionButton(text = "Далі", icon = Icons.Filled.Check, onClick = onNext)
        }
        state.correct == false -> {
            Text(
                if (state.heard.isNullOrBlank()) "Нічого не почув(-ла)" else "Почув(-ла): «${state.heard}»",
                style = MaterialTheme.typography.bodyMedium,
                color = LexumiError,
            )
            Spacer(Modifier.height(8.dp))
            Text("Правильно: ${TranslationParser.displayPrimary(card.word.term)}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                MicCircleButton(onClick = onStartListening, size = 72.dp, iconSize = 28.dp)
                PillActionButton(text = "Далі", icon = Icons.Filled.Check, onClick = onNext)
            }
        }
        else -> {
            MicCircleButton(onClick = onStartListening)
        }
    }

    if (onMarkKnown != null || onVoiceUnavailable != null) {
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFFE7E3F5))
        Spacer(Modifier.height(12.dp))
        if (onMarkKnown != null) {
            TextButton(onClick = onMarkKnown) {
                Text(
                    androidx.compose.ui.res.stringResource(com.lexumi.app.R.string.already_know) + " →",
                    color = LexumiOutline, style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        if (onVoiceUnavailable != null) {
            TextButton(onClick = onVoiceUnavailable) {
                Icon(Icons.Filled.MicOff, contentDescription = null, modifier = Modifier.size(18.dp), tint = LexumiOutline)
                Spacer(Modifier.width(6.dp))
                Text("Я зараз не можу говорити", color = LexumiOutline, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ColumnScope.MatchingGameBody(
    game: MatchingGameState,
    onSelectLeft: (Long) -> Unit,
    onSelectRight: (Long) -> Unit,
    onSpeakTerm: ((String) -> Unit)?,
    onSpeakTranslation: ((String) -> Unit)?,
) {
    Text("Знайти пару", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(8.dp))
    Text("${game.matchedIds.size} з ${game.pairs.size}", style = MaterialTheme.typography.bodyMedium, color = LexumiOutline)
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
                // Left column is the word being learned — always read in that language.
                MatchingTile(
                    text = TranslationParser.displayPrimary(pairsById.getValue(id).term),
                    selected = id == game.selectedLeftId,
                    wrongFlash = id == game.wrongFlashLeftId,
                    onClick = { onSpeakTerm?.invoke(TranslationParser.displayPrimary(pairsById.getValue(id).term)); onSelectLeft(id) },
                )
            }
        }
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(activeRight, key = { it }) { id ->
                // Right column is the native-language translation — read in the native voice, not the topic's.
                MatchingTile(
                    text = TranslationParser.displayPrimary(pairsById.getValue(id).translation),
                    selected = id == game.selectedRightId,
                    wrongFlash = id == game.wrongFlashRightId,
                    onClick = { onSpeakTranslation?.invoke(TranslationParser.displayPrimary(pairsById.getValue(id).translation)); onSelectRight(id) },
                )
            }
        }
    }
}

@Composable
private fun MatchingTile(text: String, selected: Boolean, wrongFlash: Boolean, onClick: () -> Unit) {
    val background = when {
        wrongFlash -> LexumiError.copy(alpha = 0.35f)
        selected -> LexumiOutline.copy(alpha = 0.35f)
        else -> Color.White.copy(alpha = 0.75f)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(onClick = onClick)
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
                    color = LexumiOutline,
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
                    Icon(Icons.Filled.VolumeUp, contentDescription = "Прослухати ще раз", tint = LexumiOutline)
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
