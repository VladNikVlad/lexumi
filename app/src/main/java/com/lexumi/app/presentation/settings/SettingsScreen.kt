@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lexumi.app.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.R
import com.lexumi.app.presentation.components.BackIconButton
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.PillActionButton
import com.lexumi.app.presentation.theme.LexumiError
import com.lexumi.app.presentation.theme.LexumiOutline

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onProfile: () -> Unit,
    onLoggedOut: () -> Unit,
    onDataCleared: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val savedWordsPerSession by viewModel.wordsPerSession.collectAsState()
    val savedRepetitions by viewModel.repetitions.collectAsState()
    val remindersEnabled by viewModel.remindersEnabled.collectAsState()
    val loggedOut by viewModel.loggedOut.collectAsState()
    val dataCleared by viewModel.dataCleared.collectAsState()

    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Pending (unsaved) values — words/session and repetitions only apply once
    // "Зберегти" is pressed, so a slider drag can't silently change behavior.
    var pendingWords by remember { mutableStateOf<Int?>(null) }
    var pendingRepetitions by remember { mutableStateOf<Int?>(null) }

    val displayedWords = pendingWords ?: savedWordsPerSession
    val displayedRepetitions = pendingRepetitions ?: savedRepetitions
    val hasUnsavedChanges = (pendingWords != null && pendingWords != savedWordsPerSession) ||
        (pendingRepetitions != null && pendingRepetitions != savedRepetitions)

    LaunchedEffect(loggedOut) { if (loggedOut) onLoggedOut() }
    LaunchedEffect(dataCleared) { if (dataCleared) onDataCleared() }

    GradientBackground {
        BackIconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(20.dp))
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(36.dp))
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))

            PillActionButton(
                text = stringResource(R.string.profile_section),
                icon = Icons.Filled.Person,
                onClick = onProfile,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            SettingsSection(title = "${stringResource(R.string.words_per_session)}: $displayedWords") {
                Slider(
                    value = displayedWords.toFloat(),
                    onValueChange = { pendingWords = it.toInt() },
                    valueRange = 5f..30f,
                    steps = 4,
                )
            }

            SettingsSection(title = "${stringResource(R.string.repetitions)}: $displayedRepetitions") {
                Slider(
                    value = displayedRepetitions.toFloat(),
                    onValueChange = { pendingRepetitions = it.toInt() },
                    valueRange = 1f..5f,
                    steps = 3,
                )
            }

            if (hasUnsavedChanges) {
                PillActionButton(
                    text = "Зберегти зміни",
                    icon = Icons.Filled.Check,
                    onClick = {
                        pendingWords?.let { viewModel.setWordsPerSession(it) }
                        pendingRepetitions?.let { viewModel.setRepetitions(it) }
                        pendingWords = null
                        pendingRepetitions = null
                    },
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            SettingsSection(title = stringResource(R.string.reminders)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Notifications, contentDescription = null, tint = LexumiOutline)
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = remindersEnabled, onCheckedChange = { viewModel.setRemindersEnabled(it) })
                }
            }

            Spacer(Modifier.height(12.dp))
            PillActionButton(
                text = stringResource(R.string.logout), icon = Icons.Filled.ExitToApp,
                onClick = { viewModel.logout() }, modifier = Modifier.padding(bottom = 12.dp),
            )

            if (!showDeleteConfirm) {
                TextButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = LexumiError)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.delete_all_data), color = LexumiError)
                }
            } else {
                Text(stringResource(R.string.delete_all_data_confirm), color = LexumiError, style = MaterialTheme.typography.bodyMedium)
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) }
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = { viewModel.deleteAllData() }) { Text(stringResource(R.string.yes_delete), color = LexumiError) }
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = LexumiOutline)
                Spacer(Modifier.width(8.dp))
                Text("Lexumi · " + stringResource(R.string.tagline), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
