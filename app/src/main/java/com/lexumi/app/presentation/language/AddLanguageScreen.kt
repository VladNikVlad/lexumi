package com.lexumi.app.presentation.language

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.R
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.LexumiLogo
import com.lexumi.app.presentation.components.LexumiTextField
import com.lexumi.app.presentation.components.PillActionButton

@Composable
fun AddLanguageScreen(
    onCreated: (Long) -> Unit,
    viewModel: AddLanguageViewModel = hiltViewModel(),
) {
    var name by remember { mutableStateOf("") }
    val error by viewModel.error.collectAsState()
    val createdId by viewModel.createdLanguageId.collectAsState()

    LaunchedEffect(createdId) { createdId?.let { onCreated(it) } }

    GradientBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LexumiLogo(width = 200.dp)
            Spacer(Modifier.height(40.dp))
            LexumiTextField(
                value = name,
                onValueChange = { name = it; viewModel.clearError() },
                label = stringResource(R.string.new_language_hint),
                isError = error != null,
                supportingText = error,
            )
            Spacer(Modifier.height(24.dp))
            PillActionButton(
                text = stringResource(R.string.add_language),
                icon = Icons.Filled.Check,
                onClick = { viewModel.submit(name) },
            )
        }
    }
}
