package com.example.update

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext

/** On app start: background check (if enabled, max every 12 h) and a one-tap prompt when a newer release exists. */
@Composable
fun UpdatePrompt() {
    val context = LocalContext.current
    val state by Updater.state.collectAsState()
    var dismissed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { Updater.autoCheck(context) }
    val s = state
    if (s is UpdateState.Available && !dismissed) {
        AlertDialog(
            onDismissRequest = { dismissed = true },
            title = { Text("Update available") },
            text = { Text("Version ${s.release.tag} is out. You have ${Updater.currentVersion}.") },
            confirmButton = {
                TextButton(onClick = {
                    dismissed = true
                    Updater.update(context, s.release)
                    context.startActivity(Intent(context, SettingsActivity::class.java)) // shows progress
                }) { Text("Update") }
            },
            dismissButton = { TextButton(onClick = { dismissed = true }) { Text("Later") } }
        )
    }
}
