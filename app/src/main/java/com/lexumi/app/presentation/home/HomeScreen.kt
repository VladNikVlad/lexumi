package com.lexumi.app.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.lexumi.app.presentation.components.PillActionButton
import com.lexumi.app.presentation.components.SettingsIconButton

@Composable
fun HomeScreen(
    onSelfStudy: () -> Unit,
    onOwnMaterial: () -> Unit,
    onRepeatWords: () -> Unit,
    onContinueLast: (topicId: Long, route: String) -> Unit,
    onSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val lastSession by viewModel.lastSession.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    // "Продовжити навчання" is offered first if there's a saved session, but the 3 mode buttons
    // should still be reachable from there without a real navigation — this just reveals them
    // in place, matching how "Вибрати інший розділ" always used to work.
    var showModes by remember { mutableStateOf(false) }

    GradientBackground {
        SettingsIconButton(onClick = onSettings, modifier = Modifier.align(Alignment.TopEnd).padding(20.dp))
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LexumiLogo(width = 220.dp)
            Spacer(Modifier.height(40.dp))

            val session = lastSession
            if (session != null && !showModes) {
                PillActionButton(
                    text = stringResource(R.string.continue_learning),
                    icon = Icons.Filled.MenuBook,
                    onClick = { onContinueLast(session.topicId, session.screenRoute) },
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                PillActionButton(
                    text = stringResource(R.string.choose_other_section),
                    icon = Icons.Filled.Public,
                    onClick = { showModes = true },
                )
            } else {
                PillActionButton(
                    text = stringResource(R.string.system_learning),
                    subtitle = stringResource(R.string.system_learning_subtitle),
                    icon = Icons.Filled.School,
                    enabled = false,
                    onClick = {},
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                PillActionButton(
                    text = stringResource(R.string.self_study),
                    icon = Icons.Filled.AutoStories,
                    onClick = { viewModel.enterSelfStudy(); onSelfStudy() },
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                PillActionButton(
                    text = stringResource(R.string.own_material),
                    icon = Icons.Filled.Person,
                    onClick = onOwnMaterial,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                PillActionButton(
                    text = stringResource(R.string.repeat_words),
                    icon = Icons.Filled.Autorenew,
                    onClick = onRepeatWords,
                )
            }

            syncError?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
