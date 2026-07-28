package com.lexumi.app.presentation.learnwords

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LearnWordsScreen(
    onSessionDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: LearnWordsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val rules by viewModel.rules.collectAsState()
    WordSessionBody(
        state = state,
        onBack = onBack,
        onDone = onSessionDone,
        onSubmitChoice = viewModel::submitChoice,
        onSubmitTyped = viewModel::submitTyped,
        onAddToReview = viewModel::addCurrentToReview,
        onNext = viewModel::next,
        availableRules = rules,
        onEditWord = { term, translation, imagePath, ruleId -> viewModel.editCurrentWord(term, translation, imagePath, ruleId) },
        onDeleteWord = { viewModel.deleteCurrentWord() },
        onClearEditError = viewModel::clearEditError,
        onSpeak = viewModel::speak,
    )
}
