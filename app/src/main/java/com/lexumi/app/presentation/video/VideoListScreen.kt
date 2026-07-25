package com.lexumi.app.presentation.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.NamedListPicker
import com.lexumi.app.presentation.components.PickableItem

@Composable
fun VideoListScreen(
    onVideoClick: (Long) -> Unit,
    viewModel: VideoListViewModel = hiltViewModel(),
) {
    val videos by viewModel.videos.collectAsState()
    GradientBackground {
        NamedListPicker(
            title = "Дивитися відео",
            items = videos.map { PickableItem(it.id, it.name) },
            onItemClick = onVideoClick,
        )
    }
}
