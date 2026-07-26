@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lexumi.app.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
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
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.LexumiTextField
import com.lexumi.app.presentation.components.PillActionButton
import com.lexumi.app.presentation.theme.LexumiError
import com.lexumi.app.presentation.theme.LexumiOutline

@Composable
fun SettingsScreen(
    onLoggedOut: () -> Unit,
    onDataCleared: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val wordsPerSession by viewModel.wordsPerSession.collectAsState()
    val repetitions by viewModel.repetitions.collectAsState()
    val remindersEnabled by viewModel.remindersEnabled.collectAsState()
    val currentProfileId by viewModel.currentProfileId.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val loggedOut by viewModel.loggedOut.collectAsState()
    val dataCleared by viewModel.dataCleared.collectAsState()

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showNewProfileField by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var currentLanguageTag by remember { mutableStateOf(viewModel.currentAppLanguageTag()) }

    LaunchedEffect(loggedOut) { if (loggedOut) onLoggedOut() }
    LaunchedEffect(dataCleared) { if (dataCleared) onDataCleared() }

    GradientBackground {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))

            SettingsSection(title = stringResource(R.string.app_language)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Language, contentDescription = null, tint = LexumiOutline)
                    Spacer(Modifier.width(12.dp))
                    FilterChip(
                        selected = currentLanguageTag == "uk",
                        onClick = { viewModel.setAppLanguage("uk"); currentLanguageTag = "uk" },
                        label = { Text("Українська") },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    FilterChip(
                        selected = currentLanguageTag == "en",
                        onClick = { viewModel.setAppLanguage("en"); currentLanguageTag = "en" },
                        label = { Text("English") },
                    )
                }
            }

            SettingsSection(title = "${stringResource(R.string.words_per_session)}: $wordsPerSession") {
                Slider(
                    value = wordsPerSession.toFloat(),
                    onValueChange = { viewModel.setWordsPerSession(it.toInt()) },
                    valueRange = 5f..30f,
                    steps = 4,
                )
            }

            SettingsSection(title = "${stringResource(R.string.repetitions)}: $repetitions") {
                Slider(
                    value = repetitions.toFloat(),
                    onValueChange = { viewModel.setRepetitions(it.toInt()) },
                    valueRange = 1f..5f,
                    steps = 3,
                )
            }

            SettingsSection(title = stringResource(R.string.reminders)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Notifications, contentDescription = null, tint = LexumiOutline)
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = remindersEnabled, onCheckedChange = { viewModel.setRemindersEnabled(it) })
                }
            }

            SettingsSection(title = stringResource(R.string.profile_section)) {
                profiles.forEach { profile ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = profile.id == currentProfileId,
                            onClick = { viewModel.switchToProfile(profile.id) },
                        )
                        Text(profile.displayName)
                    }
                }
                if (showNewProfileField) {
                    LexumiTextField(
                        value = newProfileName, onValueChange = { newProfileName = it },
                        label = stringResource(R.string.new_profile_name_hint), modifier = Modifier.padding(top = 8.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    PillActionButton(
                        text = stringResource(R.string.create_profile),
                        icon = Icons.Filled.Add,
                        onClick = {
                            viewModel.createAndSwitchToNewProfile(newProfileName)
                            showNewProfileField = false
                            newProfileName = ""
                        },
                    )
                } else {
                    TextButton(onClick = { showNewProfileField = true }) {
                        Icon(Icons.Filled.Person, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.switch_user))
                    }
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
