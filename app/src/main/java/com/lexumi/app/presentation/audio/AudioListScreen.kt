package com.lexumi.app.presentation.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.NamedListPicker
import com.lexumi.app.presentation.components.PickableItem

@Composable
fun AudioListScreen(
    onDialogClick: (Long) -> Unit,
    viewModel: AudioListViewModel = hiltViewModel(),
) {
    val dialogs by viewModel.dialogs.collectAsState()
    GradientBackground {
        NamedListPicker(
            title = "Слухати діалоги",
            items = dialogs.map { PickableItem(it.id, it.name) },
            onItemClick = onDialogClick,
        )
    }
}
