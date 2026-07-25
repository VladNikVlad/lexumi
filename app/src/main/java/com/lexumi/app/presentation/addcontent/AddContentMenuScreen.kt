package com.lexumi.app.presentation.addcontent

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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.LexumiLogo
import com.lexumi.app.presentation.components.PillActionButton

@Composable
fun AddContentMenuScreen(
    onAddRule: () -> Unit,
    onAddWord: () -> Unit,
    onAddImage: () -> Unit,
    onAddVideo: () -> Unit,
    onAddAudioDialog: () -> Unit,
    onAddSentence: () -> Unit,
    onAddStory: () -> Unit,
    onDone: () -> Unit,
) {
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
            Spacer(Modifier.height(24.dp))
            PillActionButton(text = "Додати правило", icon = Icons.Filled.MenuBook, onClick = onAddRule, modifier = itemSpacing)
            PillActionButton(text = "Додати слово", icon = Icons.Filled.Edit, onClick = onAddWord, modifier = itemSpacing)
            PillActionButton(text = "Додати картинку", icon = Icons.Filled.Image, onClick = onAddImage, modifier = itemSpacing)
            PillActionButton(text = "Додати відео", icon = Icons.Filled.VideoLibrary, onClick = onAddVideo, modifier = itemSpacing)
            PillActionButton(text = "Додати аудіо діалог", icon = Icons.Filled.Headphones, onClick = onAddAudioDialog, modifier = itemSpacing)
            PillActionButton(text = "Додати речення", icon = Icons.Filled.TextFields, onClick = onAddSentence, modifier = itemSpacing)
            PillActionButton(text = "Додати історію", icon = Icons.Filled.AutoStories, onClick = onAddStory, modifier = itemSpacing)
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onDone) { Text("Готово, перейти до теми") }
        }
    }
}

private val itemSpacing = Modifier.padding(bottom = 14.dp)
