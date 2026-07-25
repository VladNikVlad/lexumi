package com.lexumi.app.presentation.stories

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.NamedListPicker
import com.lexumi.app.presentation.components.PickableItem

@Composable
fun StoriesListScreen(
    onStoryClick: (Long) -> Unit,
    viewModel: StoriesListViewModel = hiltViewModel(),
) {
    val stories by viewModel.stories.collectAsState()
    GradientBackground {
        NamedListPicker(
            title = "Читати історії",
            items = stories.map { PickableItem(it.id, it.name) },
            onItemClick = onStoryClick,
        )
    }
}
