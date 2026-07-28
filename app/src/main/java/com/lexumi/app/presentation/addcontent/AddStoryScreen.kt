@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lexumi.app.presentation.addcontent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.presentation.components.BackIconButton
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.LexumiLogo
import com.lexumi.app.presentation.components.LexumiTextField
import com.lexumi.app.presentation.components.PillActionButton
import com.lexumi.app.presentation.components.RuleMultiSelect

@Composable
fun AddStoryScreen(
    onCreated: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddStoryViewModel = hiltViewModel(),
) {
    var name by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var translation by remember { mutableStateOf("") }
    var selectedRuleIds by remember { mutableStateOf(setOf<Long>()) }

    val error by viewModel.error.collectAsState()
    val created by viewModel.created.collectAsState()
    val rules by viewModel.rules.collectAsState()

    LaunchedEffect(created) { if (created) onCreated() }

    GradientBackground {
        BackIconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(20.dp))
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LexumiLogo(width = 160.dp)
            Spacer(Modifier.height(16.dp))
            Text("Додати історію", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(20.dp))

            LexumiTextField(value = name, onValueChange = { name = it; viewModel.clearError() }, label = "Назва")
            Spacer(Modifier.height(12.dp))
            LexumiTextField(value = text, onValueChange = { text = it; viewModel.clearError() }, label = "Текст історії", singleLine = false)
            Spacer(Modifier.height(12.dp))
            LexumiTextField(value = translation, onValueChange = { translation = it }, label = "Переклад (необов'язково)", singleLine = false)
            Spacer(Modifier.height(12.dp))

            RuleMultiSelect(
                rules = rules,
                selectedIds = selectedRuleIds,
                onToggle = { id -> selectedRuleIds = if (id in selectedRuleIds) selectedRuleIds - id else selectedRuleIds + id },
            )
            Spacer(Modifier.height(16.dp))

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            PillActionButton(
                text = "Додати історію",
                icon = Icons.Filled.Check,
                onClick = { viewModel.submit(name, text, translation.ifBlank { null }, selectedRuleIds.toList()) },
            )
        }
    }
}
