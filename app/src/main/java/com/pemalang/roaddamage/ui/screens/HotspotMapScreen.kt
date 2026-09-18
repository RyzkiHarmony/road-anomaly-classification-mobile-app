package com.pemalang.roaddamage.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.pemalang.roaddamage.data.remote.AnomalyPointDto
import com.pemalang.roaddamage.ui.components.BottomNavBar
import com.pemalang.roaddamage.ui.components.NavDestination
import com.pemalang.roaddamage.ui.theme.*

// ── Design Tokens ──
private val SurfaceBg = md_theme_Surface
private val CardBg = md_theme_SurfaceContainerLowest
private val Primary = md_theme_Primary
private val PrimaryLight = md_theme_PrimaryFixed
private val OnSurface = md_theme_OnSurface
private val OnSurfaceVariant = md_theme_OnSurfaceVariant
private val SurfaceContainer = md_theme_SurfaceContainer
private val StatusRed = md_theme_StatusRed
private val StatusOrange = md_theme_StatusOrange
private val StatusGreen = md_theme_StatusGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotspotMapScreen(
    onNavigateHome: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateSettings: () -> Unit,
    vm: HotspotViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val ctx = LocalContext.current

    // Center of Pemalang Regency dataset
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            com.pemalang.roaddamage.util.AppConstants.PEMALANG_CENTER,
            com.pemalang.roaddamage.util.AppConstants.DEFAULT_MAP_ZOOM
        )
    }

    // Auto-fit bounds once anomalies are loaded
    var hasFittedBounds by remember { mutableStateOf(false) }
    LaunchedEffect(ui.anomalies) {
        if (ui.anomalies.isNotEmpty() && !hasFittedBounds) {
            val validPoints = ui.anomalies.filter { it.latitude != 0.0 && it.longitude != 0.0 }
            if (validPoints.isNotEmpty()) {
                val boundsBuilder = LatLngBounds.builder()
                validPoints.forEach {
                    boundsBuilder.include(LatLng(it.latitude, it.longitude))
                }
                try {
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120),
                        durationMs = 1200
                    )
                    hasFittedBounds = true
                } catch (_: Exception) {
                    // Map layout may not be ready yet
                }
            }
        }
    }

    Scaffold(
        containerColor = SurfaceBg,
        bottomBar = {
            BottomNavBar(
                selected = NavDestination.Hotspot,
                onNavigate = { dest ->
                    when (dest) {
                        NavDestination.Home -> onNavigateHome()
                        NavDestination.History -> onNavigateHistory()
                        NavDestination.Settings -> onNavigateSettings()
                        else -> {}
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Google Map View ──
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    mapToolbarEnabled = false,
                    compassEnabled = true
                ),
                onMapClick = {
                    vm.selectAnomaly(null)
                }
            ) {
                // Render Heatmap density circles when enabled
                if (ui.isHeatmapMode) {
                    ui.filteredAnomalies.forEach { anomaly ->
                        val auraColor = if (anomaly.isPothole) {
                            Color(0x44D32F2F) // Soft red aura
                        } else {
                            Color(0x44F57C00) // Soft amber aura
                        }
                        val coreColor = if (anomaly.isPothole) {
                            Color(0x77C62828) // Dense red core
                        } else {
                            Color(0x77EF6C00) // Dense orange core
                        }

                        // Outer aura circle
                        Circle(
                            center = LatLng(anomaly.latitude, anomaly.longitude),
                            radius = 70.0,
                            fillColor = auraColor,
                            strokeColor = Color.Transparent
                        )
                        // Inner hot core
                        Circle(
                            center = LatLng(anomaly.latitude, anomaly.longitude),
                            radius = 32.0,
                            fillColor = coreColor,
                            strokeColor = Color.Transparent
                        )
                    }
                }

                // Render Anomaly Markers
                ui.filteredAnomalies.forEach { anomaly ->
                    val hue = if (anomaly.isPothole) {
                        BitmapDescriptorFactory.HUE_RED
                    } else {
                        BitmapDescriptorFactory.HUE_ORANGE
                    }

                    Marker(
                        state = MarkerState(position = LatLng(anomaly.latitude, anomaly.longitude)),
                        icon = BitmapDescriptorFactory.defaultMarker(hue),
                        title = anomaly.displayName,
                        snippet = "Confidence: ${(anomaly.confidence * 100).toInt()}%",
                        onClick = {
                            vm.selectAnomaly(anomaly)
                            true
                        }
                    )
                }
            }

            // ── Top Header & Filter Overlay ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PrimaryLight.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "GIS Hotspot Kerusakan",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                                Text(
                                    text = "Server PostgreSQL • ${ui.totalCount} Titik Terdeteksi",
                                    fontSize = 12.sp,
                                    color = OnSurfaceVariant
                                )
                            }
                        }

                        // Refresh button
                        IconButton(
                            onClick = { vm.loadAnomalies() },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainer)
                        ) {
                            if (ui.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Filter & Heatmap Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chip: Semua
                    FilterChip(
                        selected = ui.filter == HotspotFilter.ALL,
                        onClick = { vm.setFilter(HotspotFilter.ALL) },
                        label = { Text("Semua (${ui.totalCount})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary,
                            selectedLabelColor = Color.White
                        )
                    )

                    // Chip: Lubang (Pothole)
                    FilterChip(
                        selected = ui.filter == HotspotFilter.POTHOLE,
                        onClick = { vm.setFilter(HotspotFilter.POTHOLE) },
                        label = { Text("🔴 Lubang (${ui.potholeCount})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusRed,
                            selectedLabelColor = Color.White
                        )
                    )

                    // Chip: Polisi Tidur (Speed Bump)
                    FilterChip(
                        selected = ui.filter == HotspotFilter.SPEED_BUMP,
                        onClick = { vm.setFilter(HotspotFilter.SPEED_BUMP) },
                        label = { Text("🟠 Polisi Tidur (${ui.speedBumpCount})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusOrange,
                            selectedLabelColor = Color.White
                        )
                    )

                    // Chip: Heatmap Toggle
                    FilterChip(
                        selected = ui.isHeatmapMode,
                        onClick = { vm.toggleHeatmap() },
                        label = {
                            Text(if (ui.isHeatmapMode) "🔥 Heatmap: ON" else "🔥 Heatmap: OFF")
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFD32F2F),
                            selectedLabelColor = Color.White
                        )
                    )
                }

                // Error Message banner if any
                if (ui.errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = md_theme_ErrorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = md_theme_Error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = ui.errorMessage ?: "",
                                color = md_theme_OnErrorContainer,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // ── Floating Anomaly Detail Card (at bottom when a pin is selected) ──
            AnimatedVisibility(
                visible = ui.selectedAnomaly != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                ui.selectedAnomaly?.let { anomaly ->
                    AnomalyDetailCard(
                        anomaly = anomaly,
                        streetAddress = ui.selectedAddress,
                        ragState = ui.ragState,
                        onRequestRag = { vm.requestRagContext(anomaly) },
                        onClose = { vm.selectAnomaly(null) },
                        onOpenInMaps = { lat, lng ->
                            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(anomaly.displayName)})")
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            try {
                                ctx.startActivity(mapIntent)
                            } catch (_: Exception) {
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnomalyDetailCard(
    anomaly: AnomalyPointDto,
    streetAddress: String?,
    ragState: RagState,
    onRequestRag: () -> Unit,
    onClose: () -> Unit,
    onOpenInMaps: (Double, Double) -> Unit
) {
    val isPothole = anomaly.isPothole
    val accentColor = if (isPothole) StatusRed else StatusOrange

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header Row: Title & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPothole) Icons.Default.Warning else Icons.Default.ReportProblem,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = anomaly.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = OnSurface
                        )
                        Text(
                            text = "Trip: ${anomaly.tripId.take(8)}...",
                            fontSize = 11.sp,
                            color = OnSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = OnSurfaceVariant)
                }
            }

            Spacer(Modifier.height(14.dp))

            // Metrics row: Confidence & Sensor Magnitude
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Confidence Chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceContainer,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("AI Confidence", fontSize = 11.sp, color = OnSurfaceVariant)
                        Text(
                            text = "${(anomaly.confidence * 100).toInt()}%",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }

                // Sensor Magnitude Chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceContainer,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Guncangan Sensor", fontSize = 11.sp, color = OnSurfaceVariant)
                        Text(
                            text = if (anomaly.sensorMagnitude > 0) "%.2f G".format(anomaly.sensorMagnitude) else "N/A",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Reverse Geocoded Street Name & GPS Coordinates Badge
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceContainer.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = streetAddress ?: "Mendeteksi nama jalan...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "GPS: %.5f, %.5f".format(anomaly.latitude, anomaly.longitude),
                            fontSize = 11.sp,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Action: Open in External Maps
            OutlinedButton(
                onClick = { onOpenInMaps(anomaly.latitude, anomaly.longitude) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Buka di Google Maps", fontSize = 13.sp)
            }

            Spacer(Modifier.height(12.dp))

            // ── RAG Gemini Section ──
            HorizontalDivider(color = md_theme_OutlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))

            when (ragState) {
                is RagState.Idle -> {
                    Button(
                        onClick = onRequestRag,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Analisis AI Gemini (RAG)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                is RagState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = Primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Menghubungi Gemini AI & Pgvector...",
                            fontSize = 12.sp,
                            color = Primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                is RagState.Success -> {
                    val resp = ragState.response
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceContainer
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Hasil Analisis AI Gemini",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = OnSurface
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when (resp.severity.uppercase()) {
                                        "HIGH", "CRITICAL" -> StatusRed
                                        "MEDIUM" -> StatusOrange
                                        else -> StatusGreen
                                    }
                                ) {
                                    Text(
                                        text = resp.severity,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = resp.summary,
                                fontSize = 12.sp,
                                color = OnSurface,
                                lineHeight = 16.sp
                            )

                            if (resp.recommendations.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Rekomendasi Tindakan:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnSurfaceVariant
                                )
                                resp.recommendations.forEach { item ->
                                    Text(
                                        text = "• ${item.label}",
                                        fontSize = 11.sp,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                is RagState.Error -> {
                    Text(
                        text = ragState.message,
                        color = md_theme_Error,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
