package com.lexumi.app.presentation.rules

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.lexumi.app.domain.model.Rule
import com.lexumi.app.presentation.components.BackIconButton
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.LexumiLogo
import com.lexumi.app.presentation.components.ZoomableImage

@Composable
fun RulesListScreen(
    onBack: () -> Unit,
    viewModel: RulesListViewModel = hiltViewModel(),
) {
    val rules by viewModel.rules.collectAsState()
    var selectedRule by remember { mutableStateOf<Rule?>(null) }

    GradientBackground {
        BackIconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(20.dp))
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LexumiLogo(width = 160.dp)
            Spacer(Modifier.height(8.dp))
            Text("Правила", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            if (rules.isEmpty()) {
                Text("Правил ще немає", style = MaterialTheme.typography.bodyMedium)
            }
            rules.forEach { rule ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.55f)),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { selectedRule = rule },
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(rule.name, style = MaterialTheme.typography.titleMedium)
                        if (rule.imagePath != null) {
                            Spacer(Modifier.height(8.dp))
                            // Just a small preview here — tap the card to see it full-screen and zoomable.
                            Image(
                                painter = rememberAsyncImagePainter(rule.imagePath),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)),
                            )
                        }
                        if (rule.text.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(rule.text, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                        }
                    }
                }
            }
        }

        selectedRule?.let { rule ->
            RuleDetailOverlay(rule = rule, onClose = { selectedRule = null })
        }
    }
}

/** Full-screen view of a single rule, opened by tapping it in the list — the small inline
 * preview isn't legible for a photographed textbook page, so this gives it the whole screen,
 * with the photo pinch-zoomable. */
@Composable
private fun RuleDetailOverlay(rule: Rule, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(20f)
            .background(Color(0xFF1B1730)),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Закрити", tint = Color.White)
                }
                Spacer(Modifier.width(4.dp))
                Text(rule.name, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1)
            }

            if (rule.imagePath != null) {
                // The image gets the lion's share of the screen — that's the whole point of
                // this view (being able to actually read a photographed page).
                ZoomableImage(
                    model = rule.imagePath,
                    contentDescription = rule.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(if (rule.text.isNotBlank()) 0.65f else 1f),
                )
            }

            if (rule.text.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(if (rule.imagePath != null) 0.35f else 1f)
                        .background(Color.White.copy(alpha = 0.08f))
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                ) {
                    Text(rule.text, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                }
            }
        }
    }
}
