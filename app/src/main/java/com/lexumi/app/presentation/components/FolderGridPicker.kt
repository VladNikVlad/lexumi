package com.lexumi.app.presentation.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.lexumi.app.R
import com.lexumi.app.presentation.theme.LexumiOutline

data class PickableItem(val id: Long, val name: String)

/**
 * A named folder picker used for "Вибрати розділ" / "Вибрати тему": the
 * folders are grouped two-per-row and centered as a block in the available
 * space (so one or two items sit centered, not pinned to the top-left),
 * while the "add" button stays pinned near the bottom of the screen.
 *
 * When [onReorder] is provided, long-pressing a folder picks it up (it lifts
 * with a shadow and follows the finger); releasing it over another folder
 * drops it into that spot and reports the full new order.
 */
@Composable
fun FolderGridPicker(
    items: List<PickableItem>,
    title: String,
    addLabel: String,
    onItemClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    onReorder: ((List<Long>) -> Unit)? = null,
) {
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val itemBounds = remember { mutableStateMapOf<Long, Rect>() }
    // Local order, so a drop can reorder immediately without waiting on the repository's Flow
    // to round-trip; re-synced from the real data whenever nothing is being dragged.
    var localItems by remember { mutableStateOf(items) }
    LaunchedEffect(items) { if (draggingId == null) localItems = items }

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
                if (localItems.isEmpty()) {
                    Text(stringResource(R.string.empty_list_placeholder), style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        localItems.chunked(2).forEach { rowItems ->
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                rowItems.forEach { item ->
                                    val isDragging = item.id == draggingId
                                    FolderCard(
                                        item = item,
                                        onClick = { onItemClick(item.id) },
                                        modifier = Modifier
                                            .onGloballyPositioned { coords -> itemBounds[item.id] = coords.boundsInRoot() }
                                            .graphicsLayer {
                                                if (isDragging) {
                                                    translationX = dragOffset.x
                                                    translationY = dragOffset.y
                                                    scaleX = 1.06f
                                                    scaleY = 1.06f
                                                    shadowElevation = 16f
                                                }
                                            }
                                            .zIndex(if (isDragging) 1f else 0f)
                                            .then(
                                                if (onReorder == null) {
                                                    Modifier
                                                } else {
                                                    Modifier.pointerInput(item.id, localItems) {
                                                        detectDragGesturesAfterLongPress(
                                                            onDragStart = { draggingId = item.id; dragOffset = Offset.Zero },
                                                            onDrag = { change, delta -> change.consume(); dragOffset += delta },
                                                            onDragEnd = {
                                                                val startBounds = itemBounds[item.id]
                                                                val dropPoint = startBounds?.center?.plus(dragOffset)
                                                                val targetId = dropPoint?.let { point ->
                                                                    itemBounds.entries.firstOrNull { (id, rect) -> id != item.id && rect.contains(point) }?.key
                                                                }
                                                                if (targetId != null) {
                                                                    val fromIndex = localItems.indexOfFirst { it.id == item.id }
                                                                    val toIndex = localItems.indexOfFirst { it.id == targetId }
                                                                    if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
                                                                        val reordered = localItems.toMutableList()
                                                                        val moved = reordered.removeAt(fromIndex)
                                                                        reordered.add(toIndex, moved)
                                                                        localItems = reordered
                                                                        onReorder(reordered.map { it.id })
                                                                    }
                                                                }
                                                                draggingId = null
                                                                dragOffset = Offset.Zero
                                                            },
                                                            onDragCancel = { draggingId = null; dragOffset = Offset.Zero },
                                                        )
                                                    }
                                                },
                                            ),
                                    )
                                }
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
private fun FolderCard(item: PickableItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.55f)),
        modifier = modifier.width(140.dp).height(110.dp),
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
