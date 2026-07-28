package com.lexumi.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.lexumi.app.presentation.navigation.LexumiNavGraph
import com.lexumi.app.presentation.theme.LexumiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LexumiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LexumiNavGraph()
                }
            }
        }
    }
}
