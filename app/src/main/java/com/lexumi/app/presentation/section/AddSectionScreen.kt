package com.lexumi.app.presentation.section

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.R
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
        title = stringResource(R.string.new_section_title),
        fieldLabel = stringResource(R.string.section_name_label),
        submitLabel = stringResource(R.string.add_section),
        error = error,
        onClearError = { viewModel.clearError() },
        onSubmit = { viewModel.submit(it) },
    )
}
