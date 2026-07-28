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
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lexumi.app.R
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.BackIconButton
import com.lexumi.app.presentation.components.LexumiLogo
import com.lexumi.app.presentation.components.PillActionButton

@Composable
fun AddContentMenuScreen(
    onAddRule: () -> Unit,
    onAddWord: () -> Unit,
    onBulkAddWords: () -> Unit,
    onAddImage: () -> Unit,
    onAddVideo: () -> Unit,
    onAddAudioDialog: () -> Unit,
    onAddSentence: () -> Unit,
    onBulkAddSentences: () -> Unit,
    onAddStory: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    GradientBackground {
        BackIconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(20.dp))
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
            PillActionButton(text = stringResource(R.string.add_rule), icon = Icons.Filled.MenuBook, onClick = onAddRule, modifier = itemSpacing)
            PillActionButton(text = stringResource(R.string.add_word), icon = Icons.Filled.Edit, onClick = onAddWord, modifier = itemSpacing)
            PillActionButton(text = stringResource(R.string.bulk_add_words), icon = Icons.Filled.PlaylistAdd, onClick = onBulkAddWords, modifier = itemSpacing)
            PillActionButton(text = stringResource(R.string.add_image), icon = Icons.Filled.Image, onClick = onAddImage, modifier = itemSpacing)
            PillActionButton(text = stringResource(R.string.add_video), icon = Icons.Filled.VideoLibrary, onClick = onAddVideo, modifier = itemSpacing)
            PillActionButton(text = stringResource(R.string.add_audio_dialog), icon = Icons.Filled.Headphones, onClick = onAddAudioDialog, modifier = itemSpacing)
            PillActionButton(text = stringResource(R.string.add_sentence), icon = Icons.Filled.TextFields, onClick = onAddSentence, modifier = itemSpacing)
            PillActionButton(text = stringResource(R.string.bulk_add_sentences), icon = Icons.Filled.PlaylistAdd, onClick = onBulkAddSentences, modifier = itemSpacing)
            PillActionButton(text = stringResource(R.string.add_story), icon = Icons.Filled.AutoStories, onClick = onAddStory, modifier = itemSpacing)
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onDone) { Text(stringResource(R.string.done_go_to_topic)) }
        }
    }
}

private val itemSpacing = Modifier.padding(bottom = 14.dp)
