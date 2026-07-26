package com.lexumi.app.presentation.topic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.R
import com.lexumi.app.presentation.components.FolderGridPicker
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.PickableItem

@Composable
fun TopicsScreen(
    onTopicChosen: (Long) -> Unit,
    onAddTopic: () -> Unit,
    viewModel: TopicsViewModel = hiltViewModel(),
) {
    val topics by viewModel.topics.collectAsState()

    GradientBackground {
        FolderGridPicker(
            items = topics.map { PickableItem(it.id, it.name) },
            title = stringResource(R.string.choose_topic),
            addLabel = stringResource(R.string.add_topic),
            onItemClick = onTopicChosen,
            onAddClick = onAddTopic,
        )
    }
}
