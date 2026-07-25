package com.lexumi.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

/** A named folder-style grid used for "Вибрати розділ" / topic pickers, plus an "add" pill button. */
@Composable
fun FolderGridPicker(
    items: List<PickableItem>,
    title: String,
    addLabel: String,
    onItemClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(items, key = { it.id }) { item ->
                Card(
                    onClick = { onItemClick(item.id) },
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.55f)),
                    modifier = Modifier.padding(8.dp).fillMaxWidth().height(110.dp),
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
        }
        Spacer(Modifier.height(16.dp))
        PillActionButton(text = addLabel, icon = Icons.Filled.Add, onClick = onAddClick)
    }
}
