package com.lexumi.app.presentation.language

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.R
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.LexumiLogo
import com.lexumi.app.presentation.components.PillActionButton
import com.lexumi.app.presentation.components.SettingsIconButton
import com.lexumi.app.presentation.theme.LexumiOutline

@Composable
fun LanguageMenuScreen(
    onAddLanguage: () -> Unit,
    onLanguageChosen: (Long) -> Unit,
    onSettings: () -> Unit,
    viewModel: LanguageMenuViewModel = hiltViewModel(),
) {
    val languages by viewModel.languages.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(selected) { selected?.let { onLanguageChosen(it) } }

    GradientBackground {
        SettingsIconButton(onClick = onSettings, modifier = Modifier.align(Alignment.TopEnd).padding(20.dp))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LexumiLogo(width = 220.dp)
            Spacer(Modifier.height(40.dp))

            languages.forEach { language ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                    PillActionButton(
                        text = language.name,
                        icon = Icons.Filled.MenuBook,
                        onClick = { viewModel.selectLanguage(language.id) },
                    )
                    // Admin-only: push this language (and everything under it) up as global content.
                    if (uiState.isAdmin) {
                        Spacer(Modifier.width(8.dp))
                        androidx.compose.material3.IconButton(onClick = { viewModel.publish(language.id) }, enabled = !uiState.busy) {
                            androidx.compose.material3.Icon(
                                Icons.Filled.CloudUpload,
                                contentDescription = "Опублікувати",
                                tint = if (language.remoteId != null) LexumiOutline else MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            PillActionButton(
                text = stringResource(R.string.add_language),
                icon = Icons.Filled.Add,
                onClick = onAddLanguage,
            )

            // Regular users: admin-published languages they haven't downloaded yet.
            if (!uiState.isAdmin && uiState.downloadableLanguages.isNotEmpty()) {
                Spacer(Modifier.height(32.dp))
                Text("Доступно для завантаження", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                uiState.downloadableLanguages.forEach { downloadable ->
                    PillActionButton(
                        text = downloadable.name,
                        icon = Icons.Filled.CloudDownload,
                        onClick = { viewModel.download(downloadable.remoteId) },
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            }

            if (uiState.busy) {
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator()
            }
            uiState.message?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
