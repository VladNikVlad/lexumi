@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lexumi.app.presentation.addcontent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.LexumiLogo
import com.lexumi.app.presentation.components.LexumiTextField
import com.lexumi.app.presentation.components.PillActionButton
import com.lexumi.app.presentation.components.RuleMultiSelect

@Composable
fun AddVideoScreen(
    onCreated: () -> Unit,
    viewModel: AddVideoViewModel = hiltViewModel(),
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var originalText by remember { mutableStateOf("") }
    var translationText by remember { mutableStateOf("") }
    var selectedRuleIds by remember { mutableStateOf(setOf<Long>()) }
    val questions = remember { mutableStateListOf<String>() }

    val error by viewModel.error.collectAsState()
    val created by viewModel.created.collectAsState()
    val rules by viewModel.rules.collectAsState()

    LaunchedEffect(created) { if (created) onCreated() }

    GradientBackground {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LexumiLogo(width = 160.dp)
            Spacer(Modifier.height(16.dp))
            Text("Додати відео", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(20.dp))

            LexumiTextField(value = name, onValueChange = { name = it; viewModel.clearError() }, label = "Назва")
            Spacer(Modifier.height(12.dp))
            LexumiTextField(value = url, onValueChange = { url = it; viewModel.clearError() }, label = "Посилання YouTube")
            Spacer(Modifier.height(12.dp))
            LexumiTextField(value = originalText, onValueChange = { originalText = it }, label = "Текст оригіналу (необов'язково)", singleLine = false)
            Spacer(Modifier.height(12.dp))
            LexumiTextField(value = translationText, onValueChange = { translationText = it }, label = "Переклад тексту (необов'язково)", singleLine = false)
            Spacer(Modifier.height(12.dp))

            RuleMultiSelect(
                rules = rules,
                selectedIds = selectedRuleIds,
                onToggle = { id -> selectedRuleIds = if (id in selectedRuleIds) selectedRuleIds - id else selectedRuleIds + id },
            )
            Spacer(Modifier.height(16.dp))

            Text("Контрольні питання так/ні (необов'язково)", style = MaterialTheme.typography.bodyMedium)
            questions.forEachIndexed { index, q ->
                LexumiTextField(
                    value = q, onValueChange = { questions[index] = it },
                    label = "Питання ${index + 1}", modifier = Modifier.padding(top = 8.dp),
                )
            }
            TextButton(onClick = { questions.add("") }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Додати питання")
            }
            Spacer(Modifier.height(12.dp))

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            PillActionButton(
                text = "Додати відео",
                icon = Icons.Filled.Check,
                onClick = {
                    viewModel.submit(
                        name, url, originalText.ifBlank { null }, translationText.ifBlank { null },
                        selectedRuleIds.toList(), questions.filter { it.isNotBlank() }.map { it to true },
                    )
                },
            )
        }
    }
}
