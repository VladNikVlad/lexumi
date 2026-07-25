package com.lexumi.app.presentation.topic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.presentation.components.SingleFieldFormScreen

@Composable
fun AddTopicScreen(
    onCreated: (Long) -> Unit,
    viewModel: AddTopicViewModel = hiltViewModel(),
) {
    val error by viewModel.error.collectAsState()
    val createdId by viewModel.createdId.collectAsState()
    LaunchedEffect(createdId) { createdId?.let { onCreated(it) } }

    SingleFieldFormScreen(
        title = "Нова тема",
        fieldLabel = "Назва теми",
        submitLabel = "Додати тему",
        error = error,
        onClearError = { viewModel.clearError() },
        onSubmit = { viewModel.submit(it) },
    )
}
