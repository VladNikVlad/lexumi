package com.lexumi.app.presentation.section

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.presentation.components.SingleFieldFormScreen

@Composable
fun AddSectionScreen(
    onCreated: (Long) -> Unit,
    viewModel: AddSectionViewModel = hiltViewModel(),
) {
    val error by viewModel.error.collectAsState()
    val createdId by viewModel.createdId.collectAsState()
    LaunchedEffect(createdId) { createdId?.let { onCreated(it) } }

    SingleFieldFormScreen(
        title = "Новий розділ",
        fieldLabel = "Назва розділу",
        submitLabel = "Додати розділ",
        error = error,
        onClearError = { viewModel.clearError() },
        onSubmit = { viewModel.submit(it) },
    )
}
