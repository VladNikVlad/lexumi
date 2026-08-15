package com.lexumi.app.presentation.topic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.R
import com.lexumi.app.presentation.components.BackIconButton
import com.lexumi.app.presentation.components.FolderGridPicker
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.PickableItem
import com.lexumi.app.presentation.components.TopEndIconButton

@Composable
fun TopicsScreen(
    onTopicChosen: (Long) -> Unit,
    onAddTopic: () -> Unit,
    onStats: () -> Unit,
    onBack: () -> Unit,
    viewModel: TopicsViewModel = hiltViewModel(),
) {
    val topics by viewModel.topics.collectAsState()

    GradientBackground {
        BackIconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(20.dp))
        TopEndIconButton(icon = Icons.Filled.BarChart, contentDescription = "Статистика", onClick = onStats, modifier = Modifier.align(Alignment.TopEnd).padding(20.dp))
        FolderGridPicker(
            items = topics.map { PickableItem(it.id, it.name) },
            title = stringResource(R.string.choose_topic),
            addLabel = stringResource(R.string.add_topic),
            onItemClick = onTopicChosen,
            onAddClick = onAddTopic,
            onReorder = viewModel::reorder,
        )
    }
}
