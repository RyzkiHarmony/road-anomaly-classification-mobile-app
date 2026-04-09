package com.pemalang.roaddamage.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pemalang.roaddamage.ui.components.BottomNavBar
import com.pemalang.roaddamage.ui.components.NavDestination

import com.pemalang.roaddamage.ui.theme.md_theme_DarkBg
import com.pemalang.roaddamage.ui.theme.md_theme_CardBg
import com.pemalang.roaddamage.ui.theme.md_theme_AccentGreen
import com.pemalang.roaddamage.ui.theme.md_theme_TextPrimary
import com.pemalang.roaddamage.ui.theme.md_theme_TextSecondary

// ── Design tokens ──
private val DarkBg = md_theme_DarkBg
private val CardBg = md_theme_CardBg
private val AccentGreen = md_theme_AccentGreen
private val TextPrimary = md_theme_TextPrimary
private val TextSecondary = md_theme_TextSecondary

/**
 * The "idle" dashboard shown when no recording is active.
 *
 * Displays summary statistics (trips, distance), sensor telemetry,
 * and the start-recording button.
 */
@Composable
fun DashboardScreen(
    onStartRecording: () -> Unit,
    onOpenTrips: () -> Unit,
    onOpenSettings: () -> Unit,
    totalTrips: Int,
    totalDist: Float,
    accelX: Float,
    accelY: Float,
    samplingRate: Int,
    sensitivity: Float,
    eventCount: Int,
    cameraPreview: @Composable () -> Unit
) {
    Scaffold(
        containerColor = DarkBg,
        bottomBar = {
            BottomNavBar(
                selected = NavDestination.Home,
                onNavigate = { dest ->
                    when (dest) {
                        NavDestination.History -> onOpenTrips()
                        NavDestination.Settings -> onOpenSettings()
                        else -> { /* already on Home */ }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "POTHOLE DETECTOR",
                    color = AccentGreen,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(8.dp)
                            .background(Color.Green, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "SYSTEM ONLINE",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Camera / Status Card ──
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(180.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    cameraPreview()

                    Column(
                        modifier = Modifier.align(Alignment.CenterStart).padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("CALIBRATING SENSORS", color = AccentGreen, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Ready to Scan",
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(32.dp)
                            .background(Color.Transparent)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Start Recording Button ──
            Button(
                onClick = onStartRecording,
                modifier = Modifier.fillMaxWidth()
                    .height(140.dp)
                    .border(1.dp, Color(0xFF333846), RoundedCornerShape(16.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(64.dp).background(AccentGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Start",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Start Recording",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("${samplingRate}Hz Sampling Rate", color = TextSecondary, fontSize = 12.sp)
                    Text("Threshold: %.1f G".format(sensitivity), color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Detected: $eventCount", color = AccentGreen, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Stats Row ──
            Row(modifier = Modifier.fillMaxWidth()) {
                // Total Trips
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(100.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("TOTAL TRIPS", color = TextSecondary, fontSize = 10.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$totalTrips",
                            color = TextPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                // Distance
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(100.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("DISTANCE", color = TextSecondary, fontSize = 10.sp)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "%.1f".format(totalDist / 1000f),
                                color = TextPrimary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "km",
                                color = AccentGreen,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Sensor Telemetry ──
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Settings,
                            null,
                            tint = TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "SENSOR TELEMETRY",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("ACCELEROMETER X", color = TextSecondary, fontSize = 10.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("%.2f".format(accelX), color = TextPrimary)
                        }
                        Column {
                            Text("ACCELEROMETER Y", color = TextSecondary, fontSize = 10.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("%.2f".format(accelY), color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}
