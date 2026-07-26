package com.lexumi.app.presentation.section

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
fun SectionsScreen(
    onSectionChosen: (Long) -> Unit,
    onAddSection: () -> Unit,
    viewModel: SectionsViewModel = hiltViewModel(),
) {
    val sections by viewModel.sections.collectAsState()

    GradientBackground {
        FolderGridPicker(
            items = sections.map { PickableItem(it.id, it.name) },
            title = stringResource(R.string.choose_section),
            addLabel = stringResource(R.string.add_section),
            onItemClick = onSectionChosen,
            onAddClick = onAddSection,
        )
    }
}
