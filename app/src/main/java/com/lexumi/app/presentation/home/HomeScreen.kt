package com.lexumi.app.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    onLearn: () -> Unit,
    onAddSection: () -> Unit,
    onRepeatWords: () -> Unit,
    onContinueLast: (topicId: Long, route: String) -> Unit,
    onChooseOtherSection: () -> Unit,
    onSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val lastSession by viewModel.lastSession.collectAsState()

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
            if (session != null) {
                PillActionButton(
                    text = stringResource(R.string.continue_learning),
                    icon = Icons.Filled.MenuBook,
                    onClick = { onContinueLast(session.topicId, session.screenRoute) },
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                PillActionButton(
                    text = stringResource(R.string.choose_other_section),
                    icon = Icons.Filled.Public,
                    onClick = onChooseOtherSection,
                )
            } else {
                PillActionButton(
                    text = stringResource(R.string.learn),
                    icon = Icons.Filled.School,
                    onClick = onLearn,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                PillActionButton(
                    text = stringResource(R.string.add_new_section),
                    subtitle = stringResource(R.string.add_new_section_subtitle),
                    icon = Icons.Filled.Add,
                    onClick = onAddSection,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                PillActionButton(
                    text = stringResource(R.string.repeat_words),
                    icon = Icons.Filled.Autorenew,
                    onClick = onRepeatWords,
                )
            }
        }
    }
}
