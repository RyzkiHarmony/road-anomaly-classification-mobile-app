package com.pemalang.roaddamage.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.pemalang.roaddamage.model.UploadStatus
import com.pemalang.roaddamage.model.AnomalyEvent
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

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
private val StatusGreen = md_theme_StatusGreen
private val StatusOrange = md_theme_StatusOrange
private val StatusRed = md_theme_StatusRed
private val ErrorColor = md_theme_Error
private val GraphLine = md_theme_Primary

@Composable
fun TripDetailScreen(tripId: String, onBack: () -> Unit = {}) {
    val vm: TripDetailViewModel = hiltViewModel()
    val ui by vm.ui.collectAsState()
    val host = remember { SnackbarHostState() }
    val showDeleteDialog = remember { mutableStateOf(false) }
    val selectedAnomaly = remember { mutableStateOf<AnomalyEvent?>(null) }
    val ctx = LocalContext.current

    LaunchedEffect(tripId) {
        vm.load(tripId)
    }

    LaunchedEffect(vm.events) {
        vm.events.collect { event ->
            when (event) {
                is TripDetailViewModel.Event.Deleted -> onBack()
                is TripDetailViewModel.Event.Error -> host.showSnackbar(event.message)
                is TripDetailViewModel.Event.Saved -> host.showSnackbar(event.path)
                is TripDetailViewModel.Event.Share -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    ctx.startActivity(Intent.createChooser(shareIntent, "Share Trip Data"))
                }
            }
        }
    }

    Scaffold(
            containerColor = SurfaceBg,
            snackbarHost = { SnackbarHost(hostState = host) },
            topBar = {
                val trip = ui.trip
                val dateStr =
                        if (trip != null) {
                            SimpleDateFormat("MMM dd, HH:mm a", Locale.getDefault())
                                    .format(Date(trip.startTime))
                        } else ""

                Row(
                        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OnSurface)
                    }
                    Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                                text = "Trip #${tripId.takeLast(6).uppercase()}",
                                color = OnSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                        )
                        Text(text = dateStr, color = OnSurfaceVariant, fontSize = 12.sp)
                    }
                    Row {
                        IconButton(onClick = { vm.shareTrip() }) {
                            Icon(Icons.Default.Share, "Share", tint = OnSurface)
                        }
                        IconButton(onClick = { vm.saveToDownloads() }) {
                            Icon(Icons.Default.Download, "Download", tint = OnSurface)
                        }
                    }
                }
            }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            val trip = ui.trip

            if (trip != null) {
                // Map Section
                Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    MapSection(
                            ctx = ctx,
                            points = ui.points,
                            anomalyEvents = ui.anomalyEvents,
                            onAnomalyClick = { selectedAnomaly.value = it }
                    )

                    // Overlay stats on map bottom
                    Row(
                            modifier =
                                    Modifier.align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DetailStatCard("DISTANCE", "%.1f".format(trip.distance / 1000f), "KM")
                        DetailStatCard("DURATION", "${trip.duration / 60}m", "TIME")
                    }
                }

                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    // G-Force Monitor
                    Card(
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(180.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                            Icons.Filled.BarChart,
                                            null,
                                            tint = Primary,
                                            modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                            "Vertical G-Force (Y-Axis)",
                                            color = OnSurface,
                                            fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Accelerometer Y-Axis", color = OnSurfaceVariant, fontSize = 10.sp)

                            Spacer(modifier = Modifier.height(16.dp))

                            // Graph
                            VerticalGraph(
                                    values = ui.verticalG,
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                            )
                        }
                    }


                    // Upload Section
                    val isUploaded = trip.uploadStatus == UploadStatus.UPLOADED
                    Card(
                            colors =
                                    CardDefaults.cardColors(
                                            containerColor =
                                                    if (isUploaded) StatusGreen.copy(alpha = 0.08f)
                                                    else StatusOrange.copy(alpha = 0.08f)
                                    ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(70.dp),
                            border =
                                    androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isUploaded) StatusGreen.copy(alpha = 0.2f)
                                            else StatusOrange.copy(alpha = 0.2f)
                                    )
                    ) {
                        Row(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                    modifier =
                                            Modifier.size(40.dp)
                                                    .background(
                                                            if (isUploaded)
                                                                    StatusGreen.copy(alpha = 0.15f)
                                                            else StatusOrange.copy(alpha = 0.15f),
                                                            CircleShape
                                                    ),
                                    contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                        if (isUploaded) Icons.Default.CloudDone
                                        else Icons.Default.CloudUpload,
                                        null,
                                        tint = if (isUploaded) StatusGreen else StatusOrange
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        if (isUploaded) "Unggah Selesai" else "Menunggu Unggah",
                                        color = OnSurface,
                                        fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                        if (isUploaded) "Data tersinkronisasi dengan server"
                                        else "Data tersimpan secara lokal",
                                        color = OnSurfaceVariant,
                                        fontSize = 10.sp
                                )
                            }
                            if (!isUploaded) {
                                Button(
                                        onClick = { vm.enqueueUpload() },
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor = Primary
                                                ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding =
                                                PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                            "Unggah Sekarang",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                    onClick = { showDeleteDialog.value = true },
                                    modifier =
                                            Modifier.size(32.dp)
                                                    .background(
                                                            SurfaceContainer,
                                                            RoundedCornerShape(8.dp)
                                                    )
                            ) {
                                Icon(
                                        Icons.Default.Delete,
                                        "Hapus",
                                        tint = OnSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
        }

        if (showDeleteDialog.value) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog.value = false },
                containerColor = CardBg,
                title = { Text("Hapus Perjalanan", color = OnSurface, fontWeight = FontWeight.SemiBold) },
                text = { Text("Apakah Anda yakin ingin menghapus data perjalanan ini secara permanen dari perangkat?", color = OnSurfaceVariant) },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog.value = false
                            vm.deleteTrip()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorColor)
                    ) {
                        Text("Hapus", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog.value = false }) {
                        Text("Batal", color = OnSurfaceVariant)
                    }
                }
            )
        }

        if (selectedAnomaly.value != null) {
            AnomalyDetailDialog(
                anomaly = selectedAnomaly.value!!,
                onDismiss = { selectedAnomaly.value = null },
                onOpenInMaps = { lat, lng ->
                    try {
                        val uri = "geo:$lat,$lng?q=$lat,$lng"
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri))
                        ctx.startActivity(intent)
                    } catch (e: Exception) {
                        // ignore fallback
                    }
                }
            )
        }
    }
}

@Composable
fun DetailStatCard(label: String, value: String, unit: String, highlight: Boolean = false) {
    Card(
            colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.95f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.width(100.dp).height(80.dp)
    ) {
        Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                    text = value,
                    color = if (highlight) StatusOrange else OnSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
            )
            Text(text = unit, color = OnSurfaceVariant, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                    text = label,
                    color = if (highlight) StatusOrange else OnSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun markerDrawable(ctx: Context, color: Int): BitmapDrawable {
    val size = 32
    val bm = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(bm)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.style = Paint.Style.FILL
    p.color = color
    val r = RectF(4f, 4f, size - 4f, size - 4f)
    c.drawOval(r, p)
    p.style = Paint.Style.STROKE
    p.strokeWidth = 3f
    p.color = AndroidColor.WHITE
    c.drawOval(r, p)
    return BitmapDrawable(ctx.resources, bm)
}

@Composable
private fun MapSection(
        ctx: Context,
        points: List<Pair<Double, Double>>,
        anomalyEvents: List<AnomalyEvent> = emptyList(),
        onAnomalyClick: (AnomalyEvent) -> Unit
) {
    val appCtx = ctx.applicationContext
    val base = remember { java.io.File(appCtx.cacheDir, "osmdroid") }
    val tiles = remember { java.io.File(base, "tiles") }
    LaunchedEffect(Unit) {
        if (!base.exists()) base.mkdirs()
        if (!tiles.exists()) tiles.mkdirs()
        Configuration.getInstance().osmdroidBasePath = base
        Configuration.getInstance().osmdroidTileCache = tiles
        Configuration.getInstance().userAgentValue = appCtx.packageName
    }
    val mapView = remember {
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            // Light theme: NO color filter applied — standard map tiles
        }
    }
    androidx.compose.ui.viewinterop.AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView },
            update = {
                mapView.overlays.clear()
                if (points.isNotEmpty()) {
                    val geoPoints = points.map { GeoPoint(it.first, it.second) }
                    val polyline =
                            Polyline().apply {
                                outlinePaint.color = AndroidColor.parseColor("#0F5238") // Forest Green (Primary)
                                outlinePaint.strokeWidth = 8f
                                setPoints(geoPoints)
                            }
                    mapView.overlays.add(polyline)
                    val startMarker =
                            Marker(mapView).apply {
                                position = geoPoints.first()
                                title = "Start"
                                icon = markerDrawable(ctx, AndroidColor.parseColor("#0F5238"))
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            }
                    val endMarker =
                            Marker(mapView).apply {
                                position = geoPoints.last()
                                title = "End"
                                icon = markerDrawable(ctx, AndroidColor.parseColor("#D32F2F")) // Vibrant Red
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            }
                    mapView.overlays.add(startMarker)
                    mapView.overlays.add(endMarker)
                    
                    // Add anomaly markers
                    anomalyEvents.forEach { event ->
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(event.latitude, event.longitude)
                            title = null
                            snippet = null
                            val color = if (event.anomalyType == "Pothole") {
                                AndroidColor.parseColor("#D32F2F") // Vibrant Red for Pothole
                            } else {
                                AndroidColor.parseColor("#EF6C00") // Vibrant Orange for Speed Bump
                            }
                            icon = markerDrawable(ctx, color)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            setOnMarkerClickListener { _, _ ->
                                onAnomalyClick(event)
                                true
                            }
                        }
                        mapView.overlays.add(marker)
                    }

                    // Center map
                    if (geoPoints.isNotEmpty()) {
                        val boundingBox = org.osmdroid.util.BoundingBox.fromGeoPoints(geoPoints)
                        // We need to post this to run after layout
                        mapView.post { mapView.zoomToBoundingBox(boundingBox, true, 100) }
                    }
                }
            }
    )
}

@Composable
private fun VerticalGraph(values: List<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Draw grid lines — light grey-green per DESIGN.md
        val rows = 4
        val cols = 6
        val rowH = h / rows
        val colW = w / cols

        // Horizontal grid
        for (i in 0..rows) {
            val y = i * rowH
            drawLine(
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(w, y),
                    color = Color(0x22424940), // subtle on-surface-variant
                    strokeWidth = 1f
            )
        }

        // Vertical grid
        for (i in 0..cols) {
            val x = i * colW
            drawLine(
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, h),
                    color = Color(0x22424940),
                    strokeWidth = 1f
            )
        }

        if (values.isEmpty()) return@Canvas
        
        // Draw graph
        val path = Path()
        val maxVal = 20f 
        val stepX = w / (values.size - 1).coerceAtLeast(1)

        values.forEachIndexed { i, v ->
            val x = i * stepX
            // Normalize value (0..maxVal) to (h..0)
            val y = h - ((v.coerceIn(0f, maxVal) / maxVal) * h)

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path = path, color = GraphLine, style = Stroke(width = 3f))
    }
}

@Composable
private fun AnomalyDetailDialog(
    anomaly: AnomalyEvent,
    onDismiss: () -> Unit,
    onOpenInMaps: (Double, Double) -> Unit
) {
    val isPothole = anomaly.anomalyType.equals("Pothole", ignoreCase = true)
    val titleText = if (isPothole) "Lubang Jalan (Pothole)" else "Polisi Tidur (Speed Bump)"
    val statusColor = if (isPothole) StatusRed else StatusOrange
    val icon = if (isPothole) Icons.Default.Warning else Icons.Default.ReportProblem

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(statusColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = titleText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Timestamp
                val timeStr = remember(anomaly.timestamp) {
                    SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
                        .format(Date(anomaly.timestamp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = timeStr,
                        fontSize = 12.sp,
                        color = OnSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = OutlineVar.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                // Detail Items
                // 1. Confidence Meter
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tingkat Keyakinan",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnSurfaceVariant
                        )
                        Text(
                            text = "${"%.1f".format(anomaly.confidence * 100)}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { anomaly.confidence },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = statusColor,
                        trackColor = statusColor.copy(alpha = 0.2f),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2. Coordinate Info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceContainer.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Lokasi Koordinat",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Lat: ${anomaly.latitude}",
                                fontSize = 12.sp,
                                color = OnSurface
                            )
                            Text(
                                text = "Lng: ${anomaly.longitude}",
                                fontSize = 12.sp,
                                color = OnSurface
                            )
                        }
                        IconButton(
                            onClick = {
                                onOpenInMaps(anomaly.latitude, anomaly.longitude)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Open Maps",
                                tint = Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Tutup",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

