package com.pemalang.roaddamage.ui.screens

import android.content.Context
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
import com.pemalang.roaddamage.model.CameraEvent
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
    var selectedEvent by remember { mutableStateOf<CameraEvent?>(null) }
    var showFullScreenImage by remember { mutableStateOf(false) }

    // Event Preview Dialog (Thumbnail)
    if (selectedEvent != null && !showFullScreenImage) {
        EventPreviewDialog(
                event = selectedEvent!!,
                onDismiss = { selectedEvent = null },
                onImageClick = { showFullScreenImage = true }
        )
    }

    // Full Screen Image Dialog
    if (selectedEvent != null && showFullScreenImage) {
        FullScreenImageDialog(event = selectedEvent!!, onDismiss = { showFullScreenImage = false })
    }

    if (showDeleteDialog.value) {
        AlertDialog(
                onDismissRequest = { showDeleteDialog.value = false },
                title = {
                    Text("Konfirmasi Hapus", color = TextPrimary, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                            "Apakah Anda yakin ingin menghapus data perjalanan ini? Data yang dihapus tidak dapat dikembalikan.",
                            color = TextSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                vm.deleteTrip()
                                showDeleteDialog.value = false
                            }
                    ) { Text("Hapus", color = SpikeRed, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog.value = false }) {
                        Text("Batal", color = TextSecondary)
                    }
                },
                containerColor = CardBg,
                textContentColor = TextSecondary,
                titleContentColor = TextPrimary
        )
    }

    LaunchedEffect(tripId) { vm.load(tripId) }
    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            when (e) {
                is TripDetailViewModel.Event.Deleted -> onBack()
                is TripDetailViewModel.Event.Error -> host.showSnackbar(e.message)
                is TripDetailViewModel.Event.Saved -> host.showSnackbar(e.path)
                is TripDetailViewModel.Event.Share -> {
                    val intent =
                            android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(android.content.Intent.EXTRA_STREAM, e.uri)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                    ctx.startActivity(
                            android.content.Intent.createChooser(intent, "Bagikan Perjalanan")
                    )
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
                            cameraEvents = ui.cameraEvents,
                            onEventClick = { event -> selectedEvent = event }
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
                                            "G-Force Monitor",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Accelerometer Magnitude", color = TextSecondary, fontSize = 10.sp)

                            Spacer(modifier = Modifier.height(16.dp))

                            // Graph
                            MagnitudeGraph(
                                    magnitudes = ui.magnitudes,
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                            )
                        }
                    }

                    if (ui.cameraEvents.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        // Collapsible Photo Gallery
                        PhotoGallerySection(
                                cameraEvents = ui.cameraEvents,
                                onEventClick = { event -> selectedEvent = event }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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
fun PhotoGallerySection(
        cameraEvents: List<CameraEvent>,
        onEventClick: (CameraEvent) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with toggle
            Row(
                    modifier = Modifier.fillMaxWidth()
                            .clickable { expanded = !expanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                            Icons.Default.PhotoLibrary,
                            null,
                            tint = AccentGreen,
                            modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                            "Foto Kerusakan",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                            "(${cameraEvents.size} foto)",
                            color = TextSecondary,
                            fontSize = 12.sp
                    )
                }
                Icon(
                        if (expanded) Icons.Default.ExpandLess
                        else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Minimize" else "Expand",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                )
            }

            // Collapsible grid content
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))

                // Use a Column with manual grid layout to avoid nested scroll issues
                val columns = 3
                val rows = (cameraEvents.size + columns - 1) / columns

                for (row in 0 until rows) {
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (col in 0 until columns) {
                            val index = row * columns + col
                            if (index < cameraEvents.size) {
                                val event = cameraEvents[index]
                                PhotoThumbnailItem(
                                        event = event,
                                        onClick = { onEventClick(event) },
                                        modifier = Modifier.weight(1f)
                                )
                            } else {
                                // Empty spacer to maintain grid alignment
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    if (row < rows - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoThumbnailItem(
        event: CameraEvent,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
) {
    Card(
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            shape = RoundedCornerShape(8.dp),
            modifier = modifier
                    .aspectRatio(1f)
                    .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                    path = event.imagePath,
                    contentDescription = "Foto kerusakan",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    reqWidth = 200,
                    reqHeight = 200
            )
            // G-force badge
            Box(
                    modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp)
                            .background(
                                    SpikeRed.copy(alpha = 0.85f),
                                    RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                        text = "%.1fG".format(event.triggerMagnitude),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                )
            }
            // No-GPS indicator
            if (event.latitude == null || event.longitude == null) {
                Box(
                        modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(
                                        StatusOrange.copy(alpha = 0.85f),
                                        RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                            Icons.Default.LocationOff,
                            contentDescription = "No GPS",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                    )
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
        cameraEvents: List<CameraEvent> = emptyList(),
        onEventClick: (CameraEvent) -> Unit
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

                    // Camera Markers
                    cameraEvents.forEach { event ->
                        if (event.latitude != null && event.longitude != null) {
                            val marker =
                                    Marker(mapView).apply {
                                        position = GeoPoint(event.latitude, event.longitude)
                                        title = "Foto (${event.triggerMagnitude} G)"
                                        icon =
                                                markerDrawable(
                                                        ctx,
                                                        AndroidColor.YELLOW
                                                ) // Use yellow for photos
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                        setOnMarkerClickListener { m, _ ->
                                            onEventClick(event)
                                            true
                                        }
                                    }
                            mapView.overlays.add(marker)
                        }
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
private fun MagnitudeGraph(magnitudes: List<Float>, modifier: Modifier = Modifier) {
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

        if (magnitudes.isEmpty()) return@Canvas

        // Draw graph
        val path = Path()
        val maxVal = 20f // Asumsi max G sekitar 2-3G, tapi magnitude bisa spike. Kita clamp visual.
        val stepX = w / (magnitudes.size - 1).coerceAtLeast(1)

        magnitudes.forEachIndexed { i, mag ->
            val x = i * stepX
            // Normalize mag (0..maxVal) to (h..0) - invert Y
            val y = h - ((mag.coerceIn(0f, maxVal) / maxVal) * h)

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path = path, color = GraphLine, style = Stroke(width = 3f))
    }
}

// Helper function to load bitmap asynchronously
suspend fun loadBitmapAsync(path: String, reqWidth: Int? = null, reqHeight: Int? = null): Bitmap? =
        withContext(Dispatchers.IO) {
            val file = java.io.File(path)
            if (!file.exists()) return@withContext null

            try {
                if (reqWidth != null && reqHeight != null) {
                    // First decode with inJustDecodeBounds=true to check dimensions
                    val options =
                            android.graphics.BitmapFactory.Options().apply {
                                inJustDecodeBounds = true
                            }
                    android.graphics.BitmapFactory.decodeFile(path, options)

                    // Calculate inSampleSize
                    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

                    // Decode bitmap with inSampleSize set
                    options.inJustDecodeBounds = false
                    android.graphics.BitmapFactory.decodeFile(path, options)
                } else {
                    android.graphics.BitmapFactory.decodeFile(path)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

fun calculateInSampleSize(
        options: android.graphics.BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
): Int {
    // Raw height and width of image
    val (height: Int, width: Int) = options.outHeight to options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}

@Composable
fun AsyncImage(
        path: String,
        contentDescription: String?,
        modifier: Modifier = Modifier,
        contentScale: ContentScale = ContentScale.Fit,
        reqWidth: Int? = null,
        reqHeight: Int? = null
) {
    val bitmapState =
            produceState<Bitmap?>(initialValue = null, key1 = path) {
                value = loadBitmapAsync(path, reqWidth, reqHeight)
            }

    if (bitmapState.value != null) {
        androidx.compose.foundation.Image(
                bitmap = bitmapState.value!!.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
        )
    } else {
        Box(
                modifier = modifier.background(Color.Gray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
        ) { CircularProgressIndicator(color = AccentGreen, modifier = Modifier.size(24.dp)) }
    }
}

@Composable
fun EventPreviewDialog(event: CameraEvent, onDismiss: () -> Unit, onImageClick: () -> Unit) {
    AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = CardBg,
            title = {
                Text(
                        "Detail Anomali",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                )
            },
            text = {
                Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                ) {
                    // Info
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("G-Force", color = TextSecondary, fontSize = 12.sp)
                            Text(
                                    "${event.triggerMagnitude} G",
                                    color = SpikeRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Waktu", color = TextSecondary, fontSize = 12.sp)
                            Text(
                                    SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                            .format(Date(event.timestamp)),
                                    color = TextPrimary,
                                    fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Image Thumbnail
                    val file = java.io.File(event.imagePath)
                    if (file.exists()) {
                        Box(modifier = Modifier.clickable { onImageClick() }) {
                            AsyncImage(
                                    path = event.imagePath,
                                    contentDescription = "Anomaly Image",
                                    modifier =
                                            Modifier.size(200.dp)
                                                    .background(
                                                            Color.Black,
                                                            RoundedCornerShape(8.dp)
                                                    )
                                                    .border(
                                                            1.dp,
                                                            Color.Gray,
                                                            RoundedCornerShape(8.dp)
                                                    ),
                                    contentScale = ContentScale.Crop,
                                    reqWidth = 400,
                                    reqHeight = 400
                            )
                            // Magnifier Icon Overlay
                            Icon(
                                    Icons.Default.ZoomIn,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier =
                                            Modifier.align(Alignment.BottomEnd)
                                                    .padding(8.dp)
                                                    .background(
                                                            Color.Black.copy(alpha = 0.5f),
                                                            CircleShape
                                                    )
                                                    .padding(4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                                "(Klik gambar untuk memperbesar)",
                                color = TextSecondary,
                                fontSize = 10.sp
                        )
                    } else {
                        Text("File gambar tidak ditemukan", color = SpikeRed)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Tutup", color = AccentGreen) }
            }
    )
}

@Composable
fun FullScreenImageDialog(event: CameraEvent, onDismiss: () -> Unit) {
    Dialog(
            onDismissRequest = onDismiss,
            properties =
                    DialogProperties(
                            usePlatformDefaultWidth = false,
                            decorFitsSystemWindows = false
                    )
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            val file = java.io.File(event.imagePath)
            if (file.exists()) {
                AsyncImage(
                        path = event.imagePath,
                        contentDescription = "Full Screen Image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                )
            }

            // Close Button
            IconButton(
                    onClick = onDismiss,
                    modifier =
                            Modifier.align(Alignment.TopEnd)
                                    .padding(16.dp)
                                    .statusBarsPadding()
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) }

            // Info Overlay
            Column(
                    modifier =
                            Modifier.align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(16.dp)
                                    .navigationBarsPadding()
            ) {
                Text(
                        "G-Force: ${event.triggerMagnitude} G",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                )
                Text(
                        SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
                                .format(Date(event.timestamp)),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                )
            }
        }
    }
}
