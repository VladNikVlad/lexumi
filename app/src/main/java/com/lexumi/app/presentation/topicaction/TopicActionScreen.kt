package com.lexumi.app.presentation.topicaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.LexumiLogo
import com.lexumi.app.presentation.components.PillActionButton

@Composable
fun TopicActionScreen(
    onLearnRules: () -> Unit,
    onLearnWords: () -> Unit,
    onWatchVideo: () -> Unit,
    onListenDialogs: () -> Unit,
    onReadStories: () -> Unit,
    onImageTests: () -> Unit,
    onSentences: () -> Unit,
    onAddContent: () -> Unit,
    viewModel: TopicActionViewModel = hiltViewModel(),
) {
    val availability by viewModel.availability.collectAsState()
    val topicName by viewModel.topicName.collectAsState()

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LexumiLogo(width = 180.dp)
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.Text(text = topicName, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(20.dp))

            PillActionButton(text = "Вивчати правила", icon = Icons.Filled.MenuBook, onClick = onLearnRules, modifier = spacing)
            if (availability.hasWords) PillActionButton(text = "Вчити слова", icon = Icons.Filled.Edit, onClick = onLearnWords, modifier = spacing)
            if (availability.hasVideos) PillActionButton(text = "Дивитися відео", icon = Icons.Filled.PlayArrow, onClick = onWatchVideo, modifier = spacing)
            if (availability.hasAudio) PillActionButton(text = "Слухати діалоги", icon = Icons.Filled.Headphones, onClick = onListenDialogs, modifier = spacing)
            if (availability.hasStories) PillActionButton(text = "Читати текст", icon = Icons.Filled.AutoStories, onClick = onReadStories, modifier = spacing)
            if (availability.hasImages) PillActionButton(text = "Тест по картках", icon = Icons.Filled.ViewModule, onClick = onImageTests, modifier = spacing)
            if (availability.hasSentences) PillActionButton(text = "Речення", icon = Icons.Filled.TextFields, onClick = onSentences, modifier = spacing)

            androidx.compose.material3.TextButton(onClick = onAddContent) {
                androidx.compose.material3.Text("+ Додати ще контент до теми")
            }
        }
    }
}

private val spacing = Modifier.padding(bottom = 14.dp)
