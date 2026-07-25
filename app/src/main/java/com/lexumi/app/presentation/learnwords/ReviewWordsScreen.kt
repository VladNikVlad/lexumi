package com.lexumi.app.presentation.learnwords

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ReviewWordsScreen(
    onDone: () -> Unit,
    viewModel: ReviewWordsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    WordSessionBody(
        state = state,
        onDone = onDone,
        onSubmitChoice = viewModel::submitChoice,
        onSubmitTyped = viewModel::submitTyped,
        onAddToReview = null, // already in the review list
        onNext = viewModel::next,
        doneLabel = "На головну",
    )
}
