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

// ── Design tokens (Friendly Road Detection) ──
private val SurfaceBg = md_theme_Surface
private val CardBg = md_theme_SurfaceContainerLowest
private val Primary = md_theme_Primary
private val PrimaryLight = md_theme_PrimaryFixed
private val OnSurface = md_theme_OnSurface
private val OnSurfaceVariant = md_theme_OnSurfaceVariant
private val SurfaceContainer = md_theme_SurfaceContainer
private val OutlineVar = md_theme_OutlineVariant
private val ErrorColor = md_theme_Error
private val GraphLineX = md_theme_GraphX
private val GraphLineY = md_theme_GraphY
private val GraphLineZ = md_theme_GraphZ

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
        containerColor = SurfaceBg,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Back",
                    tint = OnSurface,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "ACTIVE SESSION",
                    color = OnSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = OnSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // ── Status Bar ──
            Card(
                colors = CardDefaults.cardColors(containerColor = PrimaryLight.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, OutlineVar),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(10.dp).background(Primary, CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "RECORDING ACTIVE",
                            color = Primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (gpsActive) "GPS Locked & Recording" else "Waiting for GPS...",
                            color = OnSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Detection Dashboard (muted colors per DESIGN.md) ──
            val probPothole = anomalyProbabilities.getOrNull(1) ?: 0f
            val probSpeedBump = anomalyProbabilities.getOrNull(2) ?: 0f

            val bgColor = when {
                probPothole >= 0.75f -> md_theme_StatusRed
                probSpeedBump >= 0.7f -> md_theme_StatusOrange
                else -> md_theme_StatusGreen
            }
            val textLabel = when {
                probPothole >= 0.75f -> "LUBANG TERDETEKSI"
                probSpeedBump >= 0.7f -> "POLISI TIDUR"
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
                            fontWeight = FontWeight.SemiBold
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
                            Text("ACCELEROMETER", color = OnSurfaceVariant, fontSize = 10.sp)
                            Text(
                                "%.2f G-Force".format((ay.lastOrNull() ?: 0f) / 9.80665f),
                                color = OnSurface,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
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
                            .border(1.dp, OutlineVar, RoundedCornerShape(4.dp))
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
                        Text("DURATION", color = OnSurfaceVariant, fontSize = 10.sp)
                        Text(
                            timerText,
                            color = OnSurface,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Box(
                        modifier = Modifier.size(40.dp)
                            .background(SurfaceContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.History, null, tint = Primary) }
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
                                tint = Primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("DISTANCE", color = OnSurfaceVariant, fontSize = 10.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "%.1f".format(distanceMeters / 1000f),
                                color = OnSurface,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "km",
                                color = OnSurfaceVariant,
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
                                tint = Primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("SPEED", color = OnSurfaceVariant, fontSize = 10.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "%.0f".format(currentSpeedKmh),
                                color = OnSurface,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "km/h",
                                color = OnSurfaceVariant,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Stop Button — muted coral-red ──
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.Stop, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(
                    "STOP RECORDING",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
