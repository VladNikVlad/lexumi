package com.lexumi.app.presentation.section

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.padding
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

@Composable
fun SectionsScreen(
    onSectionChosen: (Long) -> Unit,
    onAddSection: () -> Unit,
    onBack: () -> Unit,
    viewModel: SectionsViewModel = hiltViewModel(),
) {
    val sections by viewModel.sections.collectAsState()

    GradientBackground {
        BackIconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(20.dp))
        FolderGridPicker(
            items = sections.map { PickableItem(it.id, it.name) },
            title = stringResource(R.string.choose_section),
            addLabel = stringResource(R.string.add_section),
            onItemClick = onSectionChosen,
            onAddClick = onAddSection,
        )
    }
}
