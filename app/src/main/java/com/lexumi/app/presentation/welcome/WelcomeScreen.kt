package com.lexumi.app.presentation.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
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
fun WelcomeScreen(
    onDone: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    var name by remember { mutableStateOf("") }
    val created by viewModel.created.collectAsState()

    LaunchedEffect(created) { if (created) onDone() }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LexumiLogo(width = 220.dp)
            Spacer(Modifier.height(48.dp))
            LexumiTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.welcome_name_prompt),
            )
            Spacer(Modifier.height(24.dp))
            PillActionButton(
                text = stringResource(R.string.welcome_button),
                subtitle = stringResource(R.string.welcome_button_subtitle),
                icon = Icons.Filled.Edit,
                onClick = { viewModel.createProfile(name) },
            )
        }
    }
}
