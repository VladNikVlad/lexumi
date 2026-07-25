package com.lexumi.app.presentation.addcontent

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.LexumiLogo
import com.lexumi.app.presentation.components.LexumiTextField
import com.lexumi.app.presentation.components.PillActionButton
import com.lexumi.app.util.ImageCompressor
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWordScreen(
    onCreated: () -> Unit,
    viewModel: AddWordViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var term by remember { mutableStateOf("") }
    var translation by remember { mutableStateOf("") }
    var imagePath by remember { mutableStateOf<String?>(null) }
    var selectedRuleId by remember { mutableStateOf<Long?>(null) }
    var ruleMenuExpanded by remember { mutableStateOf(false) }

    val error by viewModel.error.collectAsState()
    val created by viewModel.created.collectAsState()
    val rules by viewModel.rules.collectAsState()

    LaunchedEffect(created) { if (created) onCreated() }

    // Any picked image is automatically shrunk to fit under 100kb — no more
    // rejecting the user's photo, we just compress it to a size that works.
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val file = File(context.filesDir, "word_${System.currentTimeMillis()}.jpg")
        val result = ImageCompressor.compressToFile(context, uri, file, MAX_WORD_IMAGE_BYTES)
        if (result != null) imagePath = result.absolutePath
    }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LexumiLogo(width = 160.dp)
            Spacer(Modifier.height(16.dp))
            Text("Додати слово", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            // Error (e.g. "such a word already exists") shown right here, above
            // the word fields, so it's seen immediately instead of scrolled past.
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
            }

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.5f))
                    .clickable { pickImage.launch("image/*") },
                contentAlignment = Alignment.Center,
            ) {
                if (imagePath != null) {
                    Image(
                        painter = rememberAsyncImagePainter(imagePath),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(imageVector = Icons.Filled.ImageIcon, contentDescription = "Додати картинку")
                }
            }
            Spacer(Modifier.height(16.dp))

            LexumiTextField(value = term, onValueChange = { term = it; viewModel.clearError() }, label = "Слово")
            Spacer(Modifier.height(12.dp))
            LexumiTextField(value = translation, onValueChange = { translation = it; viewModel.clearError() }, label = "Переклад")
            Spacer(Modifier.height(12.dp))

            if (rules.isNotEmpty()) {
                ExposedDropdownMenuBox(expanded = ruleMenuExpanded, onExpandedChange = { ruleMenuExpanded = it }) {
                    LexumiTextField(
                        value = rules.firstOrNull { it.id == selectedRuleId }?.name ?: "Без правила",
                        onValueChange = {},
                        label = "Правило",
                        modifier = Modifier.menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = ruleMenuExpanded, onDismissRequest = { ruleMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Без правила") }, onClick = { selectedRuleId = null; ruleMenuExpanded = false })
                        rules.forEach { rule ->
                            DropdownMenuItem(text = { Text(rule.name) }, onClick = { selectedRuleId = rule.id; ruleMenuExpanded = false })
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            PillActionButton(
                text = "Додати слово",
                icon = Icons.Filled.Check,
                onClick = { viewModel.submit(imagePath, term, translation, selectedRuleId) },
            )
        }
    }
}
