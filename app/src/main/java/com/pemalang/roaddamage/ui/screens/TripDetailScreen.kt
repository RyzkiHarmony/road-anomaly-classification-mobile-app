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

private val DarkBg = md_theme_DarkBg
private val CardBg = md_theme_CardBg
private val AccentGreen = md_theme_AccentGreen
private val TextPrimary = md_theme_TextPrimary
private val TextSecondary = md_theme_TextSecondary
private val StatusGreen = md_theme_StatusGreen
private val StatusOrange = md_theme_StatusOrange
private val GraphLine = md_theme_AccentGreen
private val SpikeRed = md_theme_StatusRed

@Composable
fun TripDetailScreen(tripId: String, onBack: () -> Unit = {}) {
    val vm: TripDetailViewModel = hiltViewModel()
    val ui by vm.ui.collectAsState()
    val host = remember { SnackbarHostState() }
    val showDeleteDialog = remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    // State for image viewing
    
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
            containerColor = DarkBg,
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                    }
                    Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                                text = "Trip #${tripId.takeLast(6).uppercase()}",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                        )
                        Text(text = dateStr, color = TextSecondary, fontSize = 12.sp)
                    }
                    Row {
                        IconButton(onClick = { vm.shareTrip() }) {
                            Icon(Icons.Default.Share, "Share", tint = TextPrimary)
                        }
                        IconButton(onClick = { vm.saveToDownloads() }) {
                            Icon(Icons.Default.Download, "Download", tint = TextPrimary)
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
                            anomalyEvents = ui.anomalyEvents
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
                                            tint = AccentGreen,
                                            modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                            "Vertical G-Force (Y-Axis)",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Accelerometer Y-Axis", color = TextSecondary, fontSize = 10.sp)

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
                                                    if (isUploaded) StatusGreen.copy(alpha = 0.1f)
                                                    else StatusOrange.copy(alpha = 0.1f)
                                    ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(70.dp),
                            border =
                                    androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isUploaded) StatusGreen.copy(alpha = 0.3f)
                                            else StatusOrange.copy(alpha = 0.3f)
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
                                                                    StatusGreen.copy(alpha = 0.2f)
                                                            else StatusOrange.copy(alpha = 0.2f),
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
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                )
                                Text(
                                        if (isUploaded) "Data tersinkronisasi dengan server"
                                        else "Data tersimpan secara lokal",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                )
                            }
                            if (!isUploaded) {
                                Button(
                                        onClick = { vm.enqueueUpload() },
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor = AccentGreen
                                                ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding =
                                                PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                            "Unggah Sekarang",
                                            color = Color.Black,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                    onClick = { showDeleteDialog.value = true },
                                    modifier =
                                            Modifier.size(32.dp)
                                                    .background(
                                                            CardBg.copy(alpha = 0.5f),
                                                            RoundedCornerShape(8.dp)
                                                    )
                            ) {
                                Icon(
                                        Icons.Default.Delete,
                                        "Hapus",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentGreen)
                }
            }
        }
    }
}

@Composable
fun DetailStatCard(label: String, value: String, unit: String, highlight: Boolean = false) {
    Card(
            colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.9f)),
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
                    color = if (highlight) StatusOrange else TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
            )
            Text(text = unit, color = TextSecondary, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                    text = label,
                    color = if (highlight) StatusOrange else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
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
        anomalyEvents: List<com.pemalang.roaddamage.model.AnomalyEvent> = emptyList()
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

            // Dark Mode Filter
            val inverseMatrix =
                    android.graphics.ColorMatrix(
                            floatArrayOf(
                                    -1.0f,
                                    0.0f,
                                    0.0f,
                                    0.0f,
                                    255f,
                                    0.0f,
                                    -1.0f,
                                    0.0f,
                                    0.0f,
                                    255f,
                                    0.0f,
                                    0.0f,
                                    -1.0f,
                                    0.0f,
                                    255f,
                                    0.0f,
                                    0.0f,
                                    0.0f,
                                    1.0f,
                                    0.0f
                            )
                    )
            val destinationColor = android.graphics.Color.parseColor("#FF2A2A2A")
            val lr = (255.0f - android.graphics.Color.red(destinationColor)) / 255.0f
            val lg = (255.0f - android.graphics.Color.green(destinationColor)) / 255.0f
            val lb = (255.0f - android.graphics.Color.blue(destinationColor)) / 255.0f
            val grayscaleMatrix =
                    android.graphics.ColorMatrix(
                            floatArrayOf(
                                    lr,
                                    lg,
                                    lb,
                                    0f,
                                    0f,
                                    lr,
                                    lg,
                                    lb,
                                    0f,
                                    0f,
                                    lr,
                                    lg,
                                    lb,
                                    0f,
                                    0f,
                                    0f,
                                    0f,
                                    0f,
                                    1f,
                                    0f,
                            )
                    )

            // Apply simple dark filter (invert + high contrast) or just invert
            // Using a simple invert for "Dark Mode" effect on standard tiles
            val filter = android.graphics.ColorMatrixColorFilter(inverseMatrix)
            this.overlayManager.tilesOverlay.setColorFilter(filter)
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
                                outlinePaint.color = AndroidColor.parseColor("#00E676") // Green
                                outlinePaint.strokeWidth = 8f
                                setPoints(geoPoints)
                            }
                    mapView.overlays.add(polyline)
                    val startMarker =
                            Marker(mapView).apply {
                                position = geoPoints.first()
                                title = "Start"
                                icon = markerDrawable(ctx, AndroidColor.GREEN)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            }
                    val endMarker =
                            Marker(mapView).apply {
                                position = geoPoints.last()
                                title = "End"
                                icon = markerDrawable(ctx, AndroidColor.RED)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            }
                    mapView.overlays.add(startMarker)
                    mapView.overlays.add(endMarker)
                    
                    // Add anomaly markers
                    anomalyEvents.forEach { event ->
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(event.latitude, event.longitude)
                            title = "${event.anomalyType} (Conf: ${"%.2f".format(event.confidence)})"
                            val color = if (event.anomalyType == "Pothole") AndroidColor.RED else AndroidColor.YELLOW
                            icon = markerDrawable(ctx, color)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
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

        // Draw grid lines
        val rows = 4
        val cols = 6
        val rowH = h / rows
        val colW = w / cols

        val gridPaint =
                Paint().apply {
                    color = android.graphics.Color.parseColor("#33FFFFFF")
                    strokeWidth = 2f
                    style = Paint.Style.STROKE
                }

        // Horizontal grid
        for (i in 0..rows) {
            val y = i * rowH
            drawLine(
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(w, y),
                    color = Color(0x33FFFFFF),
                    strokeWidth = 1f
            )
        }

        // Vertical grid
        for (i in 0..cols) {
            val x = i * colW
            drawLine(
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, h),
                    color = Color(0x33FFFFFF),
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

