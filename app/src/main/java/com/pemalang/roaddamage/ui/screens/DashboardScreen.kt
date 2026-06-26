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
import androidx.compose.material.icons.filled.Motorcycle
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
import com.pemalang.roaddamage.ui.theme.*

// ── Design tokens (Friendly Road Detection) ──
private val SurfaceBg = md_theme_Surface
private val CardBg = md_theme_SurfaceContainerLowest  // white
private val Primary = md_theme_Primary
private val PrimaryLight = md_theme_PrimaryFixed
private val OnSurface = md_theme_OnSurface
private val OnSurfaceVariant = md_theme_OnSurfaceVariant
private val SurfaceContainer = md_theme_SurfaceContainer
private val SurfaceContainerHigh = md_theme_SurfaceContainerHigh
private val StatusOrange = md_theme_StatusOrange
private val StatusGreen = md_theme_StatusGreen

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
    eventCount: Int,
    userName: String,
    pendingUploads: Int,
    isGpsEnabled: Boolean
) {
    Scaffold(
        containerColor = SurfaceBg,
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
                            .background(PrimaryLight.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Route,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Selamat Datang,", color = OnSurfaceVariant, fontSize = 14.sp)
                        Text(userName, color = OnSurface, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                
                // Profile Icon
                Box {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainer)
                            .border(1.dp, md_theme_OutlineVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Primary,
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
                            .background(Primary)
                            .border(2.dp, SurfaceBg, CircleShape)
                    )
                }
            }

            // ── Center Text ──
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Siap Merekam?", color = OnSurface, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("Pastikan ponsel terpasang stabil", color = OnSurfaceVariant, fontSize = 16.sp)
            }

            // ── Big Start Button — concentric sage-green rings ──
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
                        .background(PrimaryLight.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                            .background(PrimaryLight.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(CircleShape)
                                .background(Primary)
                                .clickable { onStartRecording() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Start",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("MULAI", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
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
                                    tint = Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("GPS", color = OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    Text(if (isGpsEnabled) "Aktif" else "Mati", color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (isGpsEnabled) Primary else md_theme_Error))
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
                                    tint = Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("SENSOR", color = OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Aktif", color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Primary))
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
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Motorcycle, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Text("$totalTrips", color = OnSurface, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Total Perjalanan", color = OnSurfaceVariant, fontSize = 12.sp)
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
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Route, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text("%.1f".format(totalDist / 1000f), color = OnSurface, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.width(4.dp))
                                    Text("km", color = OnSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Total Jarak", color = OnSurfaceVariant, fontSize = 12.sp)
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
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(StatusOrange.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = StatusOrange, modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Upload Tertunda", color = StatusOrange, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Text("Menunggu koneksi Wi-Fi", color = OnSurfaceVariant, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("$pendingUploads", color = OnSurface, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "ACTION NEEDED",
                                    color = StatusOrange,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.border(1.dp, StatusOrange.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)
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
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(PrimaryLight.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Data Tersinkronisasi", color = Primary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Text("Semua perjalanan telah diunggah", color = OnSurfaceVariant, fontSize = 12.sp)
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
