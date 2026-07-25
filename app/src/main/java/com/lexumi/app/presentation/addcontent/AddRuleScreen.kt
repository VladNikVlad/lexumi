package com.lexumi.app.presentation.addcontent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.LexumiLogo
import com.lexumi.app.presentation.components.LexumiTextField
import com.lexumi.app.presentation.components.PillActionButton

@Composable
fun AddRuleScreen(
    onCreated: () -> Unit,
    viewModel: AddRuleViewModel = hiltViewModel(),
) {
    var name by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    val error by viewModel.error.collectAsState()
    val created by viewModel.created.collectAsState()

    LaunchedEffect(created) { if (created) onCreated() }

    GradientBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LexumiLogo(width = 160.dp)
            Spacer(Modifier.height(16.dp))
            Text("Додати правило", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(20.dp))
            LexumiTextField(value = name, onValueChange = { name = it; viewModel.clearError() }, label = "Назва правила")
            Spacer(Modifier.height(12.dp))
            LexumiTextField(
                value = text, onValueChange = { text = it; viewModel.clearError() },
                label = "Текст правила", singleLine = false,
                supportingText = error, isError = error != null,
            )
            Spacer(Modifier.height(20.dp))
            PillActionButton(text = "Додати правило", icon = Icons.Filled.Check, onClick = { viewModel.submit(name, text) })
        }
    }
}
