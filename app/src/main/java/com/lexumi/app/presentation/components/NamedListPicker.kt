package com.lexumi.app.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lexumi.app.R

@Composable
fun NamedListPicker(
    title: String,
    items: List<PickableItem>,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    // A short list (a couple of items) sitting right under the top-left back button looked
    // stuck/unbalanced — centering the group vertically fixes that, while a long list still
    // behaves exactly as before (fills the screen and scrolls; centering only matters when
    // the content is shorter than the available height in the first place).
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(top = 88.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            if (items.isEmpty()) {
                Text(stringResource(R.string.empty_list_placeholder), style = MaterialTheme.typography.bodyMedium)
            }
            items.forEach { item ->
                Card(
                    onClick = { onItemClick(item.id) },
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.55f)),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                ) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}
