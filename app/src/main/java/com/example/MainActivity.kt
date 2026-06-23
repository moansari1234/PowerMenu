package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val isServiceEnabled = mutableStateOf(false)
    private val isServiceRunning = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF0C0E12) // Dark premium cosmic background
                ) { innerPadding ->
                    PowerMenuScreen(
                        isServiceEnabled = isServiceEnabled.value,
                        isServiceRunning = isServiceRunning.value,
                        onOpenSettings = { openAccessibilitySettings(this) },
                        onTriggerPowerMenu = { triggerPowerMenu(this) },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Query the state and update our dynamic Compose state
        isServiceEnabled.value = PowerMenuAccessibilityService.isServiceEnabled(this)
        isServiceRunning.value = PowerMenuAccessibilityService.isServiceRunning()
    }

    private fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Toast.makeText(
                context, 
                "Scroll to find 'Power Menu' in Downloaded Services/Installed Apps", 
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open Accessibility Settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun triggerPowerMenu(context: Context) {
        if (PowerMenuAccessibilityService.isServiceRunning()) {
            val triggered = PowerMenuAccessibilityService.showPowerMenu()
            if (!triggered) {
                Toast.makeText(context, "Failed to open power menu. Try re-enabling service.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Please enable the Accessibility Service first", Toast.LENGTH_LONG).show()
            openAccessibilitySettings(context)
        }
    }
}

@Composable
fun PowerMenuScreen(
    isServiceEnabled: Boolean,
    isServiceRunning: Boolean,
    onOpenSettings: () -> Unit,
    onTriggerPowerMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Pulse animation configuration for the power button glow
    val infiniteTransition = rememberInfiniteTransition(label = "power_glow_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // App Title Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Power Menu",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Trigger shutdown or restart dialog without using the physical power button",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF8A99AD),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Center Pulsing Power Button Box
        Box(
            modifier = Modifier
                .size(200.dp)
                .drawBehind {
                    val glowColor = if (isServiceRunning) Color(0xFFFF3B30) else Color(0xFFFF9500)
                    drawCircle(
                        color = glowColor,
                        radius = (size.minDimension / 2.3f) * pulseScale,
                        alpha = pulseAlpha
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Interactive Power Button
            val buttonColor = if (isServiceRunning) Color(0xFFFF3B30) else Color(0xFFFF9500)
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                buttonColor.copy(alpha = 0.9f),
                                buttonColor.copy(alpha = 0.6f)
                            )
                        )
                    )
                    .border(3.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    .clickable(onClick = onTriggerPowerMenu)
                    .testTag("power_trigger_button"),
                contentAlignment = Alignment.Center
            ) {
                PowerIcon(
                    color = Color.White,
                    modifier = Modifier.size(60.dp)
                )
            }
        }

        // Tap Status Text
        Text(
            text = if (isServiceRunning) "TAP TO OPEN POWER MENU" else "SERVICE SETUP REQUIRED",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isServiceRunning) Color(0xFF34C759) else Color(0xFFFF9500),
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )

        // Status Panel Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(
                title = "Accessibility",
                status = if (isServiceEnabled) "Enabled" else "Disabled",
                icon = if (isServiceEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                color = if (isServiceEnabled) Color(0xFF34C759) else Color(0xFFFF9500),
                modifier = Modifier.weight(1f)
            )

            StatusCard(
                title = "Service State",
                status = if (isServiceRunning) "Connected" else "Waiting",
                icon = if (isServiceRunning) Icons.Default.CheckCircle else Icons.Default.Info,
                color = if (isServiceRunning) Color(0xFF34C759) else Color(0xFF8A99AD),
                modifier = Modifier.weight(1f)
            )
        }

        // Instructions or Action Buttons
        if (!isServiceRunning) {
            // Permission Instructions Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161A22)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Setup Guide",
                            tint = Color(0xFFFF9500),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Setup Guide",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        InstructionStep(number = "1", text = "Tap the 'Grant Access' button below to open system settings.")
                        InstructionStep(number = "2", text = "Look for 'Power Menu' under 'Downloaded services' or 'Installed apps'.")
                        InstructionStep(number = "3", text = "Toggle the switch to 'Use Power Menu' and grant the permission.")
                        InstructionStep(number = "4", text = "Return to this app and click the pulsing button to trigger the power options.")
                    }
                }
            }

            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9500),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp, horizontal = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("grant_permission_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Grant Access In Settings",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            // Already connected: provide helper features / shortcuts tips
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161A22)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Service is Active!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    Text(
                        text = "The application is successfully running and hooked into system APIs. You can now open the power controls instantly.",
                        fontSize = 14.sp,
                        color = Color(0xFF8A99AD),
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "💡 Handy Pro-Tip:",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "In your device's Accessibility Settings, look for 'Power Menu shortcut'. Enabling this allows you to place a persistent floating accessibility button or configure a volume keys shortcut to trigger this menu from absolutely anywhere without opening this app!",
                        fontSize = 13.sp,
                        color = Color(0xFF8A99AD),
                        lineHeight = 18.sp
                    )
                }
            }

            Button(
                onClick = onTriggerPowerMenu,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF3B30),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp, horizontal = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_menu_action_button")
            ) {
                PowerIcon(
                    color = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Open Power Options",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StatusCard(
    title: String,
    status: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161A22)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF8A99AD)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = status,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun InstructionStep(
    number: String,
    text: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF9500).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF9500)
            )
        }
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color(0xFFBAC5D5),
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun PowerIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val strokeWidth = width * 0.12f

        // Draw the partial circle (arc) representing the power button ring
        drawArc(
            color = color,
            startAngle = -225f,
            sweepAngle = 270f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            ),
            size = size.copy(width = width - strokeWidth, height = height - strokeWidth),
            topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2f, strokeWidth / 2f)
        )

        // Draw the vertical line in the center-top
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(width / 2f, strokeWidth / 2f),
            end = androidx.compose.ui.geometry.Offset(width / 2f, height * 0.45f),
            strokeWidth = strokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}
