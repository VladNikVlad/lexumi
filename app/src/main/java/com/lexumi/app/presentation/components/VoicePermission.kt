package com.lexumi.app.presentation.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Wraps [action] so it asks for the microphone permission first if it hasn't
 * been granted yet, then runs [action] once it has been — used before
 * starting the "say it out loud" voice practice for mastered words/sentences.
 */
@Composable
fun rememberMicGatedAction(action: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) action()
    }
    return {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) action() else launcher.launch(Manifest.permission.RECORD_AUDIO)
    }
}
