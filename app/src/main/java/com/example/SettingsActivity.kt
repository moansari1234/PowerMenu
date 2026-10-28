package com.example

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme

/** About screen. The app has no network access: new versions are downloaded from GitHub in the browser. */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MyApplicationTheme { SettingsScreen(onBack = { finish() }) } }
    }
}

private const val REPO_URL = "https://github.com/moansari1234/PowerMenu"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    fun open(url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
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
                    Text("${context.getString(R.string.app_name)} ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "This app never connects to the internet. To update, download the latest APK from GitHub and install it over this one.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = { open("$REPO_URL/releases/latest") }, modifier = Modifier.fillMaxWidth()) {
                        Text("Download latest version")
                    }
                    TextButton(onClick = { open(REPO_URL) }) { Text("Source code on GitHub") }
                }
            }
        }
    }
}
