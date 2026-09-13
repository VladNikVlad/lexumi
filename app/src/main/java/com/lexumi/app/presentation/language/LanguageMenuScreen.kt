package com.lexumi.app.presentation.language

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.R
import com.lexumi.app.data.sync.DownloadableLanguage
import com.lexumi.app.domain.model.Language
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.LexumiLogo
import com.lexumi.app.presentation.components.LexumiTextField
import com.lexumi.app.presentation.components.PillActionButton
import com.lexumi.app.presentation.components.SettingsIconButton
import com.lexumi.app.presentation.theme.LexumiOutline
import com.lexumi.app.presentation.theme.PillShape

/** One row of the unified, alphabetically-sorted language list — either already local (this
 * profile's own, or a previously downloaded admin one) or admin-published but not downloaded yet.
 * Selecting the latter downloads it first (see [LanguageMenuViewModel.download]); either way the
 * user just picks a name from one list, never thinks about "already have it or not" themselves. */
private sealed class LanguageMenuItem(val name: String) {
    class Local(val language: Language) : LanguageMenuItem(language.name)
    class Downloadable(val remote: DownloadableLanguage) : LanguageMenuItem(remote.name)
}

/** Publishing (long-press to push a language up as admin content) is gone — that's exclusively
 * a job for the admin web panel now (admin-web/). This screen only ever reads: pick a language,
 * or download one you don't have yet. Pulling in later admin changes happens silently in the
 * background as soon as a language is opened (HomeViewModel) — no manual "Оновити" action here. */
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
    var renamingLanguage by remember { mutableStateOf<Language?>(null) }
    var deletingLanguage by remember { mutableStateOf<Language?>(null) }

    LaunchedEffect(selected) { selected?.let { onLanguageChosen(it) } }

    // Refreshing is many sequential network requests and can take a while — if the screen turns
    // off mid-refresh, Android can suspend the process and the in-flight request fails. Keep the
    // screen awake for the duration so a long refresh doesn't get interrupted.
    val view = LocalView.current
    DisposableEffect(uiState.busy) {
        val window = (view.context as? android.app.Activity)?.window
        if (uiState.busy) window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // One flat, alphabetical list — own/already-downloaded languages and admin-published ones not
    // downloaded yet, mixed together. The user just picks a name; whether it needs downloading
    // first is an implementation detail, not something they should have to think about.
    val items = (languages.map { LanguageMenuItem.Local(it) } + uiState.downloadableLanguages.map { LanguageMenuItem.Downloadable(it) })
        .sortedBy { it.name.lowercase() }

    GradientBackground {
        SettingsIconButton(onClick = onSettings, modifier = Modifier.align(Alignment.TopEnd).padding(20.dp))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LexumiLogo(width = 220.dp)
            Spacer(Modifier.height(32.dp))
            Text(stringResource(R.string.choose_language), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))

            // Scrolls on its own, independent of the "Додати власну мову" button below — that
            // button must stay pinned at the bottom of the screen no matter how many languages
            // are in the list (never pushed off-screen, never part of the scrolling content).
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items.forEach { item ->
                    when (item) {
                        is LanguageMenuItem.Local -> {
                            val language = item.language
                            // Only a language the user created themselves can be renamed/deleted
                            // here — an admin-downloaded one's name is overwritten by the next
                            // background sync anyway (see LanguageRepository doc).
                            val isOwnLanguage = language.remoteId == null
                            LanguageRow(
                                name = language.name,
                                icon = Icons.Filled.MenuBook,
                                subtitle = if (isOwnLanguage) stringResource(R.string.own_language_label) else null,
                                onClick = { viewModel.selectLanguage(language.id) },
                                trailing = if (isOwnLanguage) {
                                    {
                                        IconButton(onClick = { renamingLanguage = language }) {
                                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit), tint = LexumiOutline)
                                        }
                                        IconButton(onClick = { deletingLanguage = language }) {
                                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), tint = LexumiOutline)
                                        }
                                    }
                                } else null,
                            )
                        }
                        is LanguageMenuItem.Downloadable -> {
                            LanguageRow(
                                name = item.remote.name,
                                icon = Icons.Filled.CloudDownload,
                                onClick = { viewModel.download(item.remote.remoteId) },
                            )
                        }
                    }
                }
            }

            if (uiState.busy) {
                Spacer(Modifier.height(8.dp))
                CircularProgressIndicator()
            }
            uiState.message?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(16.dp))
            PillActionButton(
                text = stringResource(R.string.add_language),
                icon = Icons.Filled.Add,
                onClick = onAddLanguage,
            )
        }

        renamingLanguage?.let { language ->
            var newName by remember(language.id) { mutableStateOf(language.name) }
            AlertDialog(
                onDismissRequest = { renamingLanguage = null },
                title = { Text(stringResource(R.string.rename_language_title)) },
                text = { LexumiTextField(value = newName, onValueChange = { newName = it }, label = stringResource(R.string.new_language_hint)) },
                confirmButton = {
                    TextButton(onClick = { viewModel.renameLanguage(language.id, newName); renamingLanguage = null }) {
                        Text(stringResource(R.string.save))
                    }
                },
                dismissButton = { TextButton(onClick = { renamingLanguage = null }) { Text(stringResource(R.string.cancel)) } },
            )
        }

        deletingLanguage?.let { language ->
            AlertDialog(
                onDismissRequest = { deletingLanguage = null },
                title = { Text(stringResource(R.string.delete_language_title)) },
                text = { Text(stringResource(R.string.delete_language_confirm, language.name)) },
                confirmButton = {
                    TextButton(onClick = { viewModel.deleteLanguage(language.id); deletingLanguage = null }) {
                        Text(stringResource(R.string.yes_delete))
                    }
                },
                dismissButton = { TextButton(onClick = { deletingLanguage = null }) { Text(stringResource(R.string.cancel)) } },
            )
        }
    }
}

@Composable
private fun LanguageRow(
    name: String,
    icon: ImageVector,
    onClick: () -> Unit,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(
        shape = PillShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, LexumiOutline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = LexumiOutline)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LexumiOutline)
                }
            }
            trailing?.invoke()
        }
    }
}
