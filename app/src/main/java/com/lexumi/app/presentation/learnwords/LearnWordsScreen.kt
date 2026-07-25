package com.lexumi.app.presentation.learnwords

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LearnWordsScreen(
    onSessionDone: () -> Unit,
    viewModel: LearnWordsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    WordSessionBody(
        state = state,
        onDone = onSessionDone,
        onSubmitChoice = viewModel::submitChoice,
        onSubmitTyped = viewModel::submitTyped,
        onAddToReview = viewModel::addCurrentToReview,
        onNext = viewModel::next,
    )
}
