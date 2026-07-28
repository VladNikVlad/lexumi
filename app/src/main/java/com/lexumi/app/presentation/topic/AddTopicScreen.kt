package com.lexumi.app.presentation.topic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.R
import com.lexumi.app.presentation.components.SingleFieldFormScreen

@Composable
fun AddTopicScreen(
    onCreated: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: AddTopicViewModel = hiltViewModel(),
) {
    val error by viewModel.error.collectAsState()
    val createdId by viewModel.createdId.collectAsState()
    LaunchedEffect(createdId) { createdId?.let { onCreated(it) } }

    SingleFieldFormScreen(
        title = stringResource(R.string.new_topic_title),
        fieldLabel = stringResource(R.string.topic_name_label),
        submitLabel = stringResource(R.string.add_topic),
        error = error,
        onClearError = { viewModel.clearError() },
        onSubmit = { viewModel.submit(it) },
        onBack = onBack,
    )
}
