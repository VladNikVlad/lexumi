package com.lexumi.app.presentation.profile

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.R
import com.lexumi.app.presentation.components.BackIconButton
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.LexumiTextField
import com.lexumi.app.presentation.components.PillActionButton
import com.lexumi.app.presentation.theme.LexumiOutline

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onChangeLearningLanguage: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val currentProfileId by viewModel.currentProfileId.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val currentProfile = profiles.find { it.id == currentProfileId }

    var pendingName by remember { mutableStateOf<String?>(null) }
    var pendingLanguageTag by remember { mutableStateOf(viewModel.currentAppLanguageTag()) }
    var showNewProfileField by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }

    val displayedName = pendingName ?: currentProfile?.displayName ?: ""
    val hasUnsavedChanges = (pendingName != null && pendingName != currentProfile?.displayName) ||
        (pendingLanguageTag != viewModel.currentAppLanguageTag())

    GradientBackground {
        BackIconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(20.dp))
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(36.dp))
            Text(stringResource(R.string.profile_section), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))

            ProfileSection(title = stringResource(R.string.profile_name_label)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = LexumiOutline)
                    Spacer(Modifier.width(12.dp))
                    LexumiTextField(
                        value = displayedName,
                        onValueChange = { pendingName = it },
                        label = stringResource(R.string.profile_name_label),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            ProfileSection(title = stringResource(R.string.app_language)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Language, contentDescription = null, tint = LexumiOutline)
                    Spacer(Modifier.width(12.dp))
                    FilterChip(
                        selected = pendingLanguageTag == "uk",
                        onClick = { pendingLanguageTag = "uk" },
                        label = { Text("Українська") },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    FilterChip(
                        selected = pendingLanguageTag == "en",
                        onClick = { pendingLanguageTag = "en" },
                        label = { Text("English") },
                    )
                }
            }

            if (hasUnsavedChanges) {
                PillActionButton(
                    text = stringResource(R.string.save),
                    icon = Icons.Filled.Check,
                    onClick = {
                        val nameChanged = pendingName != null && pendingName != currentProfile?.displayName
                        if (nameChanged) {
                            currentProfileId?.let { viewModel.renameProfile(it, pendingName!!) }
                        }
                        val languageChanged = pendingLanguageTag != viewModel.currentAppLanguageTag()
                        if (languageChanged) {
                            viewModel.setAppLanguage(pendingLanguageTag)
                        }
                        pendingName = null
                        if (languageChanged) {
                            (context as? Activity)?.recreate()
                        }
                    },
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            PillActionButton(
                text = stringResource(R.string.change_learning_language),
                icon = Icons.Filled.MenuBook,
                onClick = onChangeLearningLanguage,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            ProfileSection(title = null) {
                if (showNewProfileField) {
                    LexumiTextField(
                        value = newProfileName, onValueChange = { newProfileName = it },
                        label = stringResource(R.string.new_profile_name_hint),
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
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.add_new_profile))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileSection(title: String?, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
            }
            content()
        }
    }
}
