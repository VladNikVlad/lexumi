package com.lexumi.app.presentation.learnwords

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.presentation.components.rememberMicGatedAction

@Composable
fun LearnWordsScreen(
    onSessionDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: LearnWordsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val rules by viewModel.rules.collectAsState()
    val startVoiceCard = rememberMicGatedAction(viewModel::startListeningForCurrentVoiceCard)
    val startHearOnlyVoice = rememberMicGatedAction(viewModel::startListeningForHearOnlyAnswer)
    WordSessionBody(
        state = state,
        onBack = onBack,
        onDone = onSessionDone,
        onSubmitChoice = viewModel::submitChoice,
        onSubmitTyped = viewModel::submitTyped,
        onAddToReview = viewModel::addCurrentToReview,
        onAlreadyKnow = viewModel::markCurrentAsKnown,
        onNext = viewModel::next,
        onStartVoiceCard = startVoiceCard,
        onNextVoiceCard = viewModel::nextVoiceCard,
        onMarkVoiceCardKnown = viewModel::markCurrentVoiceCardAsKnown,
        onVoiceUnavailable = viewModel::voiceUnavailable,
        onSelectMatchingLeft = viewModel::selectMatchingLeft,
        onSelectMatchingRight = viewModel::selectMatchingRight,
        onStartHearOnlyVoice = startHearOnlyVoice,
        onDisableVoiceForSession = viewModel::disableVoiceForSession,
        availableRules = rules,
        onEditWord = { term, translation, imagePath, ruleId -> viewModel.editCurrentWord(term, translation, imagePath, ruleId) },
        onDeleteWord = { viewModel.deleteCurrentWord() },
        onClearEditError = viewModel::clearEditError,
        onSpeak = viewModel::speak,
        onSpeakNative = viewModel::speakNative,
    )
}
