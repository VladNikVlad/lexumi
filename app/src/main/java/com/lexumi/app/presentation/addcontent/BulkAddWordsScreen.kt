package com.lexumi.app.presentation.addcontent

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexumi.app.presentation.components.BackIconButton
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.LexumiLogo
import com.lexumi.app.presentation.components.PillActionButton
import com.lexumi.app.presentation.theme.LexumiSuccess

@Composable
fun BulkAddWordsScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: BulkAddWordsViewModel = hiltViewModel(),
) {
    val text by viewModel.text.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val summary by viewModel.summary.collectAsState()

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.scanPhoto(it) }
    }

    GradientBackground {
        BackIconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(20.dp))
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LexumiLogo(width = 150.dp)
            Spacer(Modifier.height(12.dp))
            Text("Масове додавання слів", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                "По одному слову на рядок: слово - переклад",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { viewModel.setText(it) },
                placeholder = { Text("apple - яблуко\nbook - книга\nwater - вода") },
                modifier = Modifier.fillMaxWidth().height(220.dp),
            )
            Spacer(Modifier.height(12.dp))

            PillActionButton(
                text = if (isScanning) "Розпізнавання…" else "Розпізнати з фото",
                icon = Icons.Filled.PhotoCamera,
                enabled = !isScanning,
                onClick = { pickPhoto.launch("image/*") },
            )
            Spacer(Modifier.height(12.dp))

            if (summary != null) {
                val s = summary!!
                Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f))) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Додано: ${s.added}", color = LexumiSuccess)
                        if (s.skippedDuplicate > 0) Text("Пропущено (вже існують): ${s.skippedDuplicate}")
                        if (s.skippedUnparsed > 0) Text("Не вдалось розпізнати формат: ${s.skippedUnparsed}")
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            PillActionButton(
                text = if (isSubmitting) "Додаю…" else "Додати всі",
                icon = Icons.Filled.Check,
                enabled = !isSubmitting && text.isNotBlank(),
                onClick = { viewModel.submitAll() },
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDone) { Text("Готово") }
        }
    }
}
