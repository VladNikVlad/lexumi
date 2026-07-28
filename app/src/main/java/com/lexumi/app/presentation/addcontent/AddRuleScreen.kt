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
import androidx.compose.material.icons.filled.PhotoCamera
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
import com.lexumi.app.presentation.components.BackIconButton
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.LexumiLogo
import com.lexumi.app.presentation.components.LexumiTextField
import com.lexumi.app.presentation.components.PillActionButton
import com.lexumi.app.util.ImageCompressor
import java.io.File

@Composable
fun AddRuleScreen(
    onCreated: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddRuleViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var imagePath by remember { mutableStateOf<String?>(null) }

    val error by viewModel.error.collectAsState()
    val created by viewModel.created.collectAsState()
    val recognizedText by viewModel.recognizedText.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    LaunchedEffect(created) { if (created) onCreated() }
    // When OCR finishes, drop the recognized text into the field (appended, so
    // the user's own typing — if any — isn't overwritten).
    LaunchedEffect(recognizedText) {
        recognizedText?.let {
            text = if (text.isBlank()) it else "$text\n$it"
            viewModel.consumeRecognizedText()
        }
    }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val file = File(context.filesDir, "rule_${System.currentTimeMillis()}.jpg")
        val result = ImageCompressor.compressToFile(context, uri, file, maxBytes = 300 * 1024)
        if (result != null) imagePath = result.absolutePath
        viewModel.scanPhoto(uri)
    }

    GradientBackground {
        BackIconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(20.dp))
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LexumiLogo(width = 160.dp)
            Spacer(Modifier.height(16.dp))
            Text("Додати правило", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(20.dp))

            LexumiTextField(value = name, onValueChange = { name = it; viewModel.clearError() }, label = "Назва правила")
            Spacer(Modifier.height(12.dp))

            if (imagePath != null) {
                Image(
                    painter = rememberAsyncImagePainter(imagePath),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.4f)),
                )
                Spacer(Modifier.height(12.dp))
            }

            PillActionButton(
                text = when {
                    isScanning -> "Розпізнавання тексту…"
                    imagePath == null -> "Додати фото правила"
                    else -> "Замінити фото"
                },
                icon = Icons.Filled.PhotoCamera,
                enabled = !isScanning,
                onClick = { pickPhoto.launch("image/*") },
            )
            Spacer(Modifier.height(12.dp))

            LexumiTextField(
                value = text, onValueChange = { text = it; viewModel.clearError() },
                label = "Текст правила (можна залишити порожнім, якщо є фото)", singleLine = false,
                supportingText = error, isError = error != null,
            )
            Spacer(Modifier.height(20.dp))
            PillActionButton(text = "Додати правило", icon = Icons.Filled.Check, onClick = { viewModel.submit(name, text, imagePath) })
        }
    }
}
