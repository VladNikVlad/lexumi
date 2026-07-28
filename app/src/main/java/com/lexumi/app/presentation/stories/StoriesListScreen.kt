package com.lexumi.app.presentation.stories

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.presentation.components.BackIconButton
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.NamedListPicker
import com.lexumi.app.presentation.components.PickableItem

@Composable
fun StoriesListScreen(
    onStoryClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: StoriesListViewModel = hiltViewModel(),
) {
    val stories by viewModel.stories.collectAsState()
    GradientBackground {
        BackIconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(20.dp))
        NamedListPicker(
            title = "Читати історії",
            items = stories.map { PickableItem(it.id, it.name) },
            onItemClick = onStoryClick,
        )
    }
}
