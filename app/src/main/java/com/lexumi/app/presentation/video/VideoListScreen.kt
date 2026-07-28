package com.lexumi.app.presentation.video

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
fun VideoListScreen(
    onVideoClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: VideoListViewModel = hiltViewModel(),
) {
    val videos by viewModel.videos.collectAsState()
    GradientBackground {
        BackIconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(20.dp))
        NamedListPicker(
            title = "Дивитися відео",
            items = videos.map { PickableItem(it.id, it.name) },
            onItemClick = onVideoClick,
        )
    }
}
