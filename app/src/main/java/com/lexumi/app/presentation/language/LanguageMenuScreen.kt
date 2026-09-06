@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.lexumi.app.presentation.language

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.R
import com.lexumi.app.domain.model.Language
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.LexumiLogo
import com.lexumi.app.presentation.components.PillActionButton
import com.lexumi.app.presentation.components.SettingsIconButton
import com.lexumi.app.presentation.theme.LexumiOutline
import com.lexumi.app.presentation.theme.PillShape

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

    var languagePendingPublish by remember { mutableStateOf<Language?>(null) }

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
                val canPublish = uiState.isAdmin && language.remoteId == null
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                    val interactionSource = remember { MutableInteractionSource() }
                    Surface(
                        shape = PillShape,
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, LexumiOutline),
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = LocalIndication.current,
                                onClick = { viewModel.selectLanguage(language.id) },
                                onLongClick = if (canPublish) {
                                    { languagePendingPublish = language }
                                } else null,
                            ),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.MenuBook, contentDescription = null, tint = LexumiOutline)
                            Spacer(Modifier.width(16.dp))
                            Text(language.name, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    // Admin-only: push this language (and everything under it) up as global content.
                    if (uiState.isAdmin) {
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { viewModel.publish(language.id) }, enabled = !uiState.busy) {
                            Icon(
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

        languagePendingPublish?.let { language ->
            AlertDialog(
                onDismissRequest = { languagePendingPublish = null },
                title = { Text("Додати «${language.name}» в базу даних?") },
                text = { Text("Мова стане доступною для завантаження всім користувачам.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.publish(language.id)
                        languagePendingPublish = null
                    }) { Text("Додати") }
                },
                dismissButton = {
                    TextButton(onClick = { languagePendingPublish = null }) { Text(stringResource(R.string.cancel)) }
                },
            )
        }
    }
}
