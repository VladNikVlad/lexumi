package com.lexumi.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lexumi.app.presentation.theme.LexumiOutline

data class PickableItem(val id: Long, val name: String)

/**
 * A named folder picker used for "Вибрати розділ" / "Вибрати тему": the
 * folders are grouped two-per-row and centered as a block in the available
 * space (so one or two items sit centered, not pinned to the top-left),
 * while the "add" button stays pinned near the bottom of the screen.
 */
@Composable
fun FolderGridPicker(
    items: List<PickableItem>,
    title: String,
    addLabel: String,
    onItemClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 28.dp, start = 20.dp, end = 20.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center,
            ) {
                if (items.isEmpty()) {
                    Text("Тут поки що порожньо", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items.chunked(2).forEach { rowItems ->
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                rowItems.forEach { item -> FolderCard(item, onClick = { onItemClick(item.id) }) }
                            }
                        }
                    }
                }
            }
        }

        PillActionButton(
            text = addLabel,
            icon = Icons.Filled.Add,
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp)
                .padding(bottom = 25.dp),
        )
    }
}

@Composable
private fun FolderCard(item: PickableItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.55f)),
        modifier = Modifier.width(140.dp).height(110.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(imageVector = Icons.Filled.Folder, contentDescription = null, tint = LexumiOutline)
            Spacer(Modifier.height(6.dp))
            Text(text = item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
        }
    }
}
