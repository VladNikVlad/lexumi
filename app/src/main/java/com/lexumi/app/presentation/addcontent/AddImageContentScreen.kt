package com.lexumi.app.presentation.addcontent

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import java.io.File

@Composable
fun AddImageContentScreen(
    onCreated: () -> Unit,
    viewModel: AddImageContentViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var translation by remember { mutableStateOf("") }
    var imagePath by remember { mutableStateOf<String?>(null) }
    val error by viewModel.error.collectAsState()
    val created by viewModel.created.collectAsState()

    LaunchedEffect(created) { if (created) onCreated() }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
        val file = File(context.filesDir, "image_${System.currentTimeMillis()}.jpg")
        file.writeBytes(bytes)
        imagePath = file.absolutePath
    }

    GradientBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LexumiLogo(width = 160.dp)
            Spacer(Modifier.height(16.dp))
            Text("Додати картинку", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.5f))
                    .clickable { pickImage.launch("image/*") },
                contentAlignment = Alignment.Center,
            ) {
                if (imagePath != null) {
                    Image(painter = rememberAsyncImagePainter(imagePath), contentDescription = null, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(imageVector = Icons.Filled.ImageIcon, contentDescription = "Вибрати картинку")
                }
            }
            Spacer(Modifier.height(16.dp))

            LexumiTextField(value = name, onValueChange = { name = it; viewModel.clearError() }, label = "Назва")
            Spacer(Modifier.height(12.dp))
            LexumiTextField(
                value = translation, onValueChange = { translation = it }, label = "Переклад / підпис",
                supportingText = error, isError = error != null,
            )
            Spacer(Modifier.height(20.dp))
            PillActionButton(text = "Додати картинку", icon = Icons.Filled.Check, onClick = { viewModel.submit(name, imagePath, translation) })
        }
    }
}
