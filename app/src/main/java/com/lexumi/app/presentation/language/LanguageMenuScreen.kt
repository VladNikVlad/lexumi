package com.lexumi.app.presentation.language

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.R
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.LexumiLogo
import com.lexumi.app.presentation.components.PillActionButton
import com.lexumi.app.presentation.components.SettingsIconButton
import com.lexumi.app.presentation.theme.LexumiOutline
import com.lexumi.app.presentation.theme.PillShape

/** Publishing (long-press to push a language up as admin content) is gone — that's exclusively
 * a job for the admin web panel now (admin-web/). This screen only ever reads: pick a language,
 * pull in later admin changes ("Оновити"), or download one you don't have yet. */
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

    // Refreshing is many sequential network requests and can take a while — if the screen turns
    // off mid-refresh, Android can suspend the process and the in-flight request fails. Keep the
    // screen awake for the duration so a long refresh doesn't get interrupted.
    val view = LocalView.current
    DisposableEffect(uiState.busy) {
        val window = (view.context as? android.app.Activity)?.window
        if (uiState.busy) window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

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
                    Surface(
                        shape = PillShape,
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, LexumiOutline),
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .clickable { viewModel.selectLanguage(language.id) },
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
                    // Anyone with a server-linked copy can pull later changes — e.g. content
                    // added through the admin web panel — back in.
                    if (language.remoteId != null) {
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { viewModel.refresh(language.id) }, enabled = !uiState.busy) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Оновити з сервера", tint = LexumiOutline)
                        }
                    }
                }
            }

            PillActionButton(
                text = stringResource(R.string.add_language),
                icon = Icons.Filled.Add,
                onClick = onAddLanguage,
            )

            if (uiState.downloadableLanguages.isNotEmpty()) {
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
