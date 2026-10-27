package com.example.update

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.ui.theme.MyApplicationTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MyApplicationTheme { SettingsScreen(onBack = { finish() }) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val state by Updater.state.collectAsState()
    var auto by remember { mutableStateOf(Updater.isAutoCheck(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("App updates", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${context.getString(R.string.app_name)} ${Updater.currentVersion} (${BuildConfig.BUILD_TYPE})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Check for updates automatically")
                            Text("From GitHub releases, at most twice a day", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = auto, onCheckedChange = { auto = it; Updater.setAutoCheck(context, it) })
                    }
                    HorizontalDivider()
                    UpdateStatus(state)
                    when (val s = state) {
                        is UpdateState.Available -> Button(
                            onClick = { Updater.update(context, s.release) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Update to ${s.release.tag}") }
                        is UpdateState.Downloading, UpdateState.Checking, UpdateState.Installing -> {}
                        else -> OutlinedButton(
                            onClick = { Updater.checkNow(context) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Check for updates") }
                    }
                    TextButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("${Updater.repoUrl}/releases")))
                    }) { Text("All releases on GitHub") }
                }
            }
        }
    }
}

@Composable
private fun UpdateStatus(state: UpdateState) {
    when (state) {
        UpdateState.Idle -> Text("Tap below to look for a new version.", style = MaterialTheme.typography.bodyMedium)
        UpdateState.Checking -> Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(12.dp)); Text("Checking GitHub…")
        }
        UpdateState.UpToDate -> Text("You're on the latest version.", style = MaterialTheme.typography.bodyMedium)
        is UpdateState.Available -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Update available: ${state.release.tag}", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)
            if (state.release.notes.isNotBlank()) Text(state.release.notes.take(600), style = MaterialTheme.typography.bodySmall)
        }
        is UpdateState.Downloading -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Downloading ${state.release.tag}… ${(state.progress * 100).toInt()}%")
            LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
        }
        UpdateState.Installing -> Text("Installing… confirm on the system screen.")
        is UpdateState.Failed -> Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}
