package com.pemalang.roaddamage.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
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
import com.pemalang.roaddamage.ui.components.Chart3Lines
import com.pemalang.roaddamage.ui.components.LegendItem

import com.pemalang.roaddamage.ui.theme.*

// ── Design tokens ──
private val DarkBg = md_theme_DarkBg
private val CardBg = md_theme_CardBg
private val AccentGreen = md_theme_AccentGreen
private val TextPrimary = md_theme_TextPrimary
private val TextSecondary = md_theme_TextSecondary
private val GraphLineZ = md_theme_GraphZ
private val GraphLineX = md_theme_GraphX
private val GraphLineY = md_theme_GraphY

/**
 * Full-screen UI shown while a recording session is active.
 *
 * Displays the live accelerometer chart, duration, distance, speed
 * and a stop-recording button.
 */
@Composable
fun ActiveSessionScreen(
    timerText: String,
    distanceMeters: Float,
    ax: FloatArray,
    ay: FloatArray,
    az: FloatArray,
    gpsActive: Boolean,
    currentSpeedKmh: Float,
    onStop: () -> Unit,
    anomalyProbabilities: FloatArray
) {
    Scaffold(
        containerColor = DarkBg,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "ACTIVE SESSION",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // ── Status Bar ──
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2630)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF2C3E50)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(10.dp).background(AccentGreen, CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "RECORDING ACTIVE",
                            color = AccentGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (gpsActive) "GPS Locked & Recording" else "Waiting for GPS...",
                            color = Color(0xFF455A64),
                            fontSize = 10.sp
                        )
                    }
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3240)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) { Text("Hide", fontSize = 10.sp, color = TextPrimary) }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Detection Dashboard ──
            val probPothole = anomalyProbabilities.getOrNull(1) ?: 0f
            val probSpeedBump = anomalyProbabilities.getOrNull(2) ?: 0f

            val bgColor = when {
                probPothole >= 0.4861f -> Color(0xFFC62828) // Red
                probSpeedBump >= 0.5364f -> Color(0xFFF9A825) // Yellow
                else -> Color(0xFF2E7D32) // Green
            }
            val textLabel = when {
                probPothole >= 0.4861f -> "LUBANG TERDETEKSI"
                probSpeedBump >= 0.5364f -> "POLISI TIDUR"
                else -> "JALAN NORMAL"
            }
            val subText = "Pothole: ${String.format("%.1f%%", probPothole * 100)} | Bump: ${String.format("%.1f%%", probSpeedBump * 100)}"

            Card(
                colors = CardDefaults.cardColors(containerColor = bgColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(120.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            textLabel,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            subText,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Accelerometer Graph ──
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("ACCELEROMETER", color = TextSecondary, fontSize = 10.sp)
                            Text(
                                "%.2f G-Force".format((ay.lastOrNull() ?: 0f) / 9.80665f),
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row {
                            LegendItem(GraphLineX, "X")
                            Spacer(Modifier.width(8.dp))
                            LegendItem(GraphLineY, "Y")
                            Spacer(Modifier.width(8.dp))
                            LegendItem(GraphLineZ, "Z")
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .weight(1f)
                            .border(1.dp, Color(0xFF2C3240))
                    ) { Chart3Lines(ax, ay, az, Modifier.fillMaxSize()) }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Duration Card ──
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("DURATION", color = TextSecondary, fontSize = 10.sp)
                        Text(
                            timerText,
                            color = TextPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier.size(40.dp)
                            .background(Color(0xFF2C3E50), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.History, null, tint = AccentGreen) }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Stats Row (Distance / Speed) ──
            Row(modifier = Modifier.fillMaxWidth()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PlayArrow, null,
                                tint = AccentGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("DISTANCE", color = TextSecondary, fontSize = 10.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "%.1f".format(distanceMeters / 1000f),
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "km",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.width(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PlayArrow, null,
                                tint = AccentGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("SPEED", color = TextSecondary, fontSize = 10.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "%.0f".format(currentSpeedKmh),
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "km/h",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Stop Button ──
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.Stop, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(
                    "STOP RECORDING",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
