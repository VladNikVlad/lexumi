package com.lexumi.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SingleFieldFormScreen(
    title: String,
    fieldLabel: String,
    submitLabel: String,
    error: String?,
    onClearError: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    GradientBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LexumiLogo(width = 180.dp)
            Spacer(Modifier.height(16.dp))
            androidx.compose.material3.Text(text = title, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(24.dp))
            LexumiTextField(
                value = value,
                onValueChange = { value = it; onClearError() },
                label = fieldLabel,
                isError = error != null,
                supportingText = error,
            )
            Spacer(Modifier.height(24.dp))
            PillActionButton(text = submitLabel, icon = Icons.Filled.Check, onClick = { onSubmit(value) })
        }
    }
}
