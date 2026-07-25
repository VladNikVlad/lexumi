package com.lexumi.app.presentation.rules

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.lexumi.app.presentation.components.GradientBackground
import com.lexumi.app.presentation.components.LexumiLogo

@Composable
fun RulesListScreen(
    viewModel: RulesListViewModel = hiltViewModel(),
) {
    val rules by viewModel.rules.collectAsState()

    GradientBackground {
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
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(rule.name, style = MaterialTheme.typography.titleMedium)
                        if (rule.imagePath != null) {
                            Spacer(Modifier.height(8.dp))
                            Image(
                                painter = rememberAsyncImagePainter(rule.imagePath),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)),
                            )
                        }
                        if (rule.text.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(rule.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
