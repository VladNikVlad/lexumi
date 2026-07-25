package com.lexumi.app.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lexumi.app.domain.model.Rule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleMultiSelect(
    rules: List<Rule>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (rules.isEmpty()) return
    Text(text = "Прив'язати правило(а)", modifier = Modifier.padding(bottom = 4.dp))
    LazyRow(modifier = modifier.fillMaxWidth()) {
        items(rules, key = { it.id }) { rule ->
            FilterChip(
                selected = rule.id in selectedIds,
                onClick = { onToggle(rule.id) },
                label = { Text(rule.name) },
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}
