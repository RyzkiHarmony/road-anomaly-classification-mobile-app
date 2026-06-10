package com.pemalang.roaddamage.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pemalang.roaddamage.ui.components.BottomNavBar
import com.pemalang.roaddamage.ui.components.NavDestination
import com.pemalang.roaddamage.ui.theme.md_theme_AccentGreen
import com.pemalang.roaddamage.ui.theme.md_theme_CardBg
import com.pemalang.roaddamage.ui.theme.md_theme_DarkBg
import com.pemalang.roaddamage.ui.theme.md_theme_StatusOrange
import com.pemalang.roaddamage.ui.theme.md_theme_TextPrimary
import com.pemalang.roaddamage.ui.theme.md_theme_TextSecondary

// ── Design tokens ──
private val DarkBg = md_theme_DarkBg
private val CardBg = md_theme_CardBg
private val AccentGreen = md_theme_AccentGreen
private val TextPrimary = md_theme_TextPrimary
private val TextSecondary = md_theme_TextSecondary

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
    userName: String,
    pendingUploads: Int,
    isGpsEnabled: Boolean
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top Bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Route,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Selamat Datang,", color = TextSecondary, fontSize = 14.sp)
                        Text(userName, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                // Profile Icon
                Box {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(CardBg)
                            .border(1.dp, TextSecondary.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = AccentGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    // Green dot
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = (-2).dp, y = (-2).dp)
                            .clip(CircleShape)
                            .background(AccentGreen)
                            .border(2.dp, DarkBg, CircleShape)
                    )
                }
            }

            // ── Center Text ──
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Siap Merekam?", color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Pastikan ponsel terpasang stabil", color = TextSecondary, fontSize = 16.sp)
            }

            // ── Big Start Button ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer glow effect with concentric circles
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(CircleShape)
                        .background(AccentGreen.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                            .background(AccentGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(CircleShape)
                                .background(AccentGreen)
                                .clickable { onStartRecording() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Start",
                                    tint = DarkBg,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("MULAI", color = DarkBg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ── Stats Grid ──
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                // Row 1
                Row(modifier = Modifier.fillMaxWidth()) {
                    // GPS Status
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).height(80.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.SignalCellularAlt,
                                    contentDescription = null,
                                    tint = AccentGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("GPS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(if (isGpsEnabled) "Aktif" else "Mati", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (isGpsEnabled) AccentGreen else Color.Red))
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    // Sensor Status
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).height(80.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Sensors,
                                    contentDescription = null,
                                    tint = AccentGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("SENSOR", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text("Aktif", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AccentGreen))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Row 2
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(DarkBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color(0xFF5A6B8C), modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Text("$totalTrips", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Total Perjalanan", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    // Total Distance
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).height(100.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(DarkBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Route, contentDescription = null, tint = Color(0xFF9C27B0), modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text("%.1f".format(totalDist / 1000f), color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(4.dp))
                                    Text("km", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Total Jarak", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Upload Status Card
                if (pendingUploads > 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(90.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(md_theme_StatusOrange),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Upload Tertunda", color = md_theme_StatusOrange, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("Menunggu koneksi Wi-Fi", color = TextSecondary, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("$pendingUploads", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "ACTION NEEDED",
                                    color = md_theme_StatusOrange,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.border(1.dp, md_theme_StatusOrange.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(90.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(AccentGreen.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Data Tersinkronisasi", color = AccentGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("Semua perjalanan telah diunggah", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
