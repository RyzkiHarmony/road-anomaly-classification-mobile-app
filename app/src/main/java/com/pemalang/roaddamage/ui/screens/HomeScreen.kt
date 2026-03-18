package com.pemalang.roaddamage.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.pemalang.roaddamage.recording.RecordingService
import java.io.File
import kotlinx.coroutines.delay

private val DarkBg = Color(0xFF1A1D26)
private val CardBg = Color(0xFF242834)
private val AccentGreen = Color(0xFF00E676)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8F9BB3)
private val GraphLineZ = Color(0xFF00E676)
private val GraphLineX = Color(0xFFFF4081)
private val GraphLineY = Color(0xFFFFD740)

@Composable
fun HomeScreen(
        onStartRecording: () -> Unit,
        onOpenTrips: () -> Unit,
        onOpenSettings: () -> Unit = {}
) {
        val ctx = LocalContext.current
        val vm: RecordingViewModel = hiltViewModel()
        val isRecording = vm.recording.collectAsState()
        val startTime = vm.startTime.collectAsState()
        val distance = vm.distance.collectAsState()
        val magnitudes = vm.magnitudes.collectAsState()
        val ax = vm.ax.collectAsState()
        val ay = vm.ay.collectAsState()
        val az = vm.az.collectAsState()
        val gpsActive = vm.gpsActive.collectAsState()
        val gpsAccuracy = vm.gpsAccuracy.collectAsState()
        val totalTrips = vm.totalTrips.collectAsState()
        val totalDistance = vm.totalDistance.collectAsState()
        val samplingRate = vm.samplingRate.collectAsState()
        val sensitivity = vm.sensitivityThreshold.collectAsState()
        val eventCount = vm.eventCount.collectAsState()
        val cameraTrigger = vm.cameraTrigger.collectAsState(initial = 0f)

        val lifecycleOwner = LocalLifecycleOwner.current
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(ctx) }
        val imageCapture = remember {
                ImageCapture.Builder()
                        .setResolutionSelector(
                                ResolutionSelector.Builder()
                                        .setResolutionStrategy(
                                                ResolutionStrategy(
                                                        android.util.Size(1280, 720),
                                                        ResolutionStrategy
                                                                .FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                                )
                                        )
                                        .build()
                        )
                        .build()
        }
        val camPermissionGranted = remember {
                mutableStateOf(
                        ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                                PackageManager.PERMISSION_GRANTED
                )
        }

        // Trigger Logic
        LaunchedEffect(Unit) {
                vm.cameraTrigger.collect { mag ->
                        if (mag > 0 && isRecording.value && camPermissionGranted.value) {
                                delay(300) // Anti-blur
                                try {
                                        val photoFile =
                                                File(
                                                        ctx.getExternalFilesDir(null),
                                                        "IMG_${System.currentTimeMillis()}.jpg"
                                                )
                                        val outputOptions =
                                                ImageCapture.OutputFileOptions.Builder(photoFile)
                                                        .build()
                                        imageCapture.takePicture(
                                                outputOptions,
                                                ContextCompat.getMainExecutor(ctx),
                                                object : ImageCapture.OnImageSavedCallback {
                                                        override fun onError(
                                                                exc: ImageCaptureException
                                                        ) {
                                                                Log.e(
                                                                        "Camera",
                                                                        "Capture failed",
                                                                        exc
                                                                )
                                                        }

                                                        override fun onImageSaved(
                                                                output:
                                                                        ImageCapture.OutputFileResults
                                                        ) {
                                                                vm.saveCameraEvent(
                                                                        photoFile.absolutePath,
                                                                        mag
                                                                )
                                                                Toast.makeText(
                                                                                ctx,
                                                                                "Foto diambil! (%.1f G)".format(
                                                                                        mag
                                                                                ),
                                                                                Toast.LENGTH_SHORT
                                                                        )
                                                                        .show()
                                                        }
                                                }
                                        )
                                } catch (e: Exception) {
                                        Log.e("Camera", "Error", e)
                                }
                        }
                }
        }

        // Permission State
        var showRationale by remember { mutableStateOf(false) }
        var showSettingsRedirect by remember { mutableStateOf(false) }
        var showGpsDialog by remember { mutableStateOf(false) }

        val requiredPermissions = remember {
                val perms =
                        mutableListOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.CAMERA
                        )
                if (Build.VERSION.SDK_INT >= 33) {
                        perms.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                perms.toTypedArray()
        }

        val timerText = remember { mutableStateOf("00:00") }

        val launcher =
                rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                ) { res ->
                        val locGranted =
                                res[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                                        res[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                        val camGranted = res[Manifest.permission.CAMERA] == true
                        camPermissionGranted.value = camGranted
                        val notifGranted =
                                Build.VERSION.SDK_INT < 33 ||
                                        res[Manifest.permission.POST_NOTIFICATIONS] == true

                        if (locGranted && camGranted && notifGranted) {
                                startService(ctx, RecordingService.ACTION_START)
                        } else {
                                // Check if we should show rationale (if false, it might mean
                                // permanently denied)
                                val activity = ctx as? Activity
                                val shouldShowRationale =
                                        requiredPermissions.any {
                                                activity?.shouldShowRequestPermissionRationale(
                                                        it
                                                ) == true
                                        }

                                if (!shouldShowRationale) {
                                        // Permanently denied (or first time, but we just asked) ->
                                        // Redirect to Settings
                                        showSettingsRedirect = true
                                }
                        }
                }

        // Permission Dialogs
        if (showRationale) {
                AlertDialog(
                        onDismissRequest = { showRationale = false },
                        title = { Text("Izin Diperlukan") },
                        text = {
                                Text(
                                        "Aplikasi ini membutuhkan akses lokasi dan kamera untuk merekam jejak perjalanan, mendeteksi kerusakan jalan, dan mengambil foto konteks kerusakan. Mohon izinkan akses."
                                )
                        },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                showRationale = false
                                                launcher.launch(requiredPermissions)
                                        }
                                ) { Text("Izinkan") }
                        },
                        dismissButton = {
                                TextButton(onClick = { showRationale = false }) { Text("Batal") }
                        },
                        containerColor = CardBg,
                        titleContentColor = TextPrimary,
                        textContentColor = TextSecondary
                )
        }

        if (showSettingsRedirect) {
                AlertDialog(
                        onDismissRequest = { showSettingsRedirect = false },
                        title = { Text("Izin Ditolak Permanen") },
                        text = {
                                Text(
                                        "Anda telah menolak izin lokasi secara permanen. Mohon aktifkan izin secara manual di Pengaturan Aplikasi untuk menggunakan fitur ini."
                                )
                        },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                showSettingsRedirect = false
                                                val intent =
                                                        Intent(
                                                                        AndroidSettings
                                                                                .ACTION_APPLICATION_DETAILS_SETTINGS
                                                                )
                                                                .apply {
                                                                        data =
                                                                                Uri.fromParts(
                                                                                        "package",
                                                                                        ctx.packageName,
                                                                                        null
                                                                                )
                                                                }
                                                ctx.startActivity(intent)
                                        }
                                ) { Text("Buka Pengaturan") }
                        },
                        dismissButton = {
                                TextButton(onClick = { showSettingsRedirect = false }) {
                                        Text("Batal")
                                }
                        },
                        containerColor = CardBg,
                        titleContentColor = TextPrimary,
                        textContentColor = TextSecondary
                )
        }

        if (showGpsDialog) {
                AlertDialog(
                        onDismissRequest = { showGpsDialog = false },
                        title = { Text("GPS Nonaktif") },
                        text = {
                                Text(
                                        "Fitur ini memerlukan GPS yang aktif untuk merekam lokasi. Mohon aktifkan GPS Anda."
                                )
                        },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                showGpsDialog = false
                                                val intent =
                                                        Intent(
                                                                AndroidSettings
                                                                        .ACTION_LOCATION_SOURCE_SETTINGS
                                                        )
                                                ctx.startActivity(intent)
                                        }
                                ) { Text("Buka Pengaturan") }
                        },
                        dismissButton = {
                                TextButton(onClick = { showGpsDialog = false }) { Text("Batal") }
                        },
                        containerColor = CardBg,
                        titleContentColor = TextPrimary,
                        textContentColor = TextSecondary
                )
        }

        LaunchedEffect(isRecording.value) {
                while (isRecording.value) {
                        val start = startTime.value
                        if (start > 0) {
                                val dur = (System.currentTimeMillis() - start) / 1000
                                val m = (dur % 60).toInt()
                                val h = (dur / 3600).toInt()
                                val min = ((dur / 60) % 60).toInt()
                                val s = "%02d:%02d".format(min, m)
                                timerText.value = if (h > 0) "%02d:%s".format(h, s) else s
                        }
                        delay(1000)
                }
                if (!isRecording.value) timerText.value = "00:00"
        }

        val cameraContent: @Composable () -> Unit = {
                if (camPermissionGranted.value) {
                        val previewView = remember { PreviewView(ctx) }
                        LaunchedEffect(previewView, isRecording.value) {
                                if (isRecording.value) {
                                        try {
                                                val cameraProvider = cameraProviderFuture.get()
                                                val preview = Preview.Builder().build()
                                                preview.setSurfaceProvider(
                                                        previewView.surfaceProvider
                                                )
                                                cameraProvider.unbindAll()
                                                cameraProvider.bindToLifecycle(
                                                        lifecycleOwner,
                                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                                        preview,
                                                        imageCapture
                                                )
                                        } catch (e: Exception) {
                                                Log.e("Camera", "Bind error", e)
                                        }
                                } else {
                                        try {
                                                val cameraProvider = cameraProviderFuture.get()
                                                cameraProvider.unbindAll()
                                        } catch (e: Exception) {}
                                }
                        }
                        AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
                } else {
                        Box(
                                Modifier.fillMaxSize().background(Color(0xFF2C3240)),
                                contentAlignment = Alignment.Center
                        ) {
                                Text(
                                        "Camera Permission Required",
                                        color = Color.White,
                                        fontSize = 10.sp
                                )
                        }
                }
        }

        if (!isRecording.value) {
                DashboardScreen(
                        onStartRecording = {
                                val allGranted =
                                        requiredPermissions.all {
                                                ContextCompat.checkSelfPermission(ctx, it) ==
                                                        PackageManager.PERMISSION_GRANTED
                                        }

                                if (allGranted) {
                                        val lm =
                                                ctx.getSystemService(Context.LOCATION_SERVICE) as
                                                        android.location.LocationManager
                                        if (!lm.isProviderEnabled(
                                                        android.location.LocationManager
                                                                .GPS_PROVIDER
                                                )
                                        ) {
                                                showGpsDialog = true
                                        } else {
                                                startService(ctx, RecordingService.ACTION_START)
                                        }
                                } else {
                                        val activity = ctx as? Activity
                                        val shouldShowRationale =
                                                requiredPermissions.any {
                                                        activity?.shouldShowRequestPermissionRationale(
                                                                it
                                                        ) == true
                                                }

                                        if (shouldShowRationale) {
                                                showRationale = true
                                        } else {
                                                launcher.launch(requiredPermissions)
                                        }
                                }
                        },
                        onOpenTrips = onOpenTrips,
                        onOpenSettings = onOpenSettings,
                        totalTrips = totalTrips.value,
                        totalDist = totalDistance.value ?: 0f,
                        accelX = ax.value.lastOrNull() ?: 0f,
                        accelY = ay.value.lastOrNull() ?: 0f,
                        samplingRate = samplingRate.value,
                        sensitivity = sensitivity.value,
                        eventCount = eventCount.value,
                        cameraPreview = cameraContent
                )
        } else {
                ActiveSessionScreen(
                        timerText = timerText.value,
                        distanceMeters = distance.value,
                        ax = ax.value,
                        ay = ay.value,
                        az = az.value,
                        gpsActive = gpsActive.value,
                        onStop = { startService(ctx, RecordingService.ACTION_STOP) },
                        cameraPreview = cameraContent
                )
        }
}

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
                        NavigationBar(containerColor = DarkBg, contentColor = AccentGreen) {
                                NavigationBarItem(
                                        selected = true,
                                        onClick = {},
                                        icon = { Icon(Icons.Default.Home, "Home") },
                                        label = { Text("Home") },
                                        colors =
                                                NavigationBarItemDefaults.colors(
                                                        selectedIconColor = AccentGreen,
                                                        selectedTextColor = AccentGreen,
                                                        unselectedIconColor = TextSecondary,
                                                        unselectedTextColor = TextSecondary,
                                                        indicatorColor = CardBg
                                                )
                                )
                                NavigationBarItem(
                                        selected = false,
                                        onClick = onOpenTrips,
                                        icon = { Icon(Icons.Default.History, "History") },
                                        label = { Text("History") },
                                        colors =
                                                NavigationBarItemDefaults.colors(
                                                        selectedIconColor = AccentGreen,
                                                        selectedTextColor = AccentGreen,
                                                        unselectedIconColor = TextSecondary,
                                                        unselectedTextColor = TextSecondary,
                                                        indicatorColor = CardBg
                                                )
                                )
                                NavigationBarItem(
                                        selected = false,
                                        onClick = onOpenSettings,
                                        icon = { Icon(Icons.Default.Settings, "Settings") },
                                        label = { Text("Settings") },
                                        colors =
                                                NavigationBarItemDefaults.colors(
                                                        selectedIconColor = AccentGreen,
                                                        selectedTextColor = AccentGreen,
                                                        unselectedIconColor = TextSecondary,
                                                        unselectedTextColor = TextSecondary,
                                                        indicatorColor = CardBg
                                                )
                                )
                        }
                }
        ) { padding ->
                Column(
                        modifier = Modifier.fillMaxSize()
                                .padding(padding)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        // Header
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
                                                modifier =
                                                        Modifier.size(8.dp)
                                                                .background(
                                                                        Color.Green,
                                                                        CircleShape
                                                                )
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

                        // Map Placeholder / Status
                        Card(
                                colors = CardDefaults.cardColors(containerColor = CardBg),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().height(180.dp)
                        ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                        cameraPreview()

                                        Column(
                                                modifier =
                                                        Modifier.align(Alignment.CenterStart)
                                                                .padding(16.dp)
                                        ) {
                                                Row(
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Icon(
                                                                Icons.Default
                                                                        .Settings, // Placeholder
                                                                // for loading
                                                                contentDescription = null,
                                                                tint = AccentGreen,
                                                                modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(
                                                                "CALIBRATING SENSORS",
                                                                color = AccentGreen,
                                                                fontSize = 12.sp
                                                        )
                                                }
                                                Spacer(Modifier.height(8.dp))
                                                Text(
                                                        "Ready to Scan",
                                                        color = TextPrimary,
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.Bold
                                                )
                                        }

                                        // Small graph decoration
                                        Icon(
                                                imageVector =
                                                        Icons.Default
                                                                .PlayArrow, // Just a placeholder
                                                // icon or shape
                                                contentDescription = null,
                                                tint = AccentGreen,
                                                modifier =
                                                        Modifier.align(Alignment.BottomEnd)
                                                                .padding(16.dp)
                                                                .size(32.dp)
                                                                .background(
                                                                        Color.Transparent
                                                                ) // Remove bg
                                        )
                                }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Start Recording Button
                        Button(
                                onClick = onStartRecording,
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .height(140.dp)
                                                .border(
                                                        1.dp,
                                                        Color(0xFF333846),
                                                        RoundedCornerShape(16.dp)
                                                ),
                                colors = ButtonDefaults.buttonColors(containerColor = CardBg),
                                shape = RoundedCornerShape(16.dp)
                        ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                                modifier =
                                                        Modifier.size(64.dp)
                                                                .background(
                                                                        AccentGreen,
                                                                        CircleShape
                                                                ),
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
                                        Text(
                                                "${samplingRate}Hz Sampling Rate",
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                        )
                                        Text(
                                                "Threshold: %.1f G".format(sensitivity),
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                                "Detected: $eventCount",
                                                color = AccentGreen,
                                                fontSize = 12.sp
                                        )
                                }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Stats Row
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
                                                Text(
                                                        "TOTAL TRIPS",
                                                        color = TextSecondary,
                                                        fontSize = 10.sp
                                                )
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
                                                Text(
                                                        "DISTANCE",
                                                        color = TextSecondary,
                                                        fontSize = 10.sp
                                                )
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
                                                                modifier =
                                                                        Modifier.padding(
                                                                                bottom = 6.dp
                                                                        )
                                                        )
                                                }
                                        }
                                }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Sensor Telemetry
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
                                                        Text(
                                                                "ACCELEROMETER X",
                                                                color = TextSecondary,
                                                                fontSize = 10.sp
                                                        )
                                                        Spacer(Modifier.height(4.dp))
                                                        Text(
                                                                "%.2f".format(accelX),
                                                                color = TextPrimary
                                                        )
                                                }
                                                Column {
                                                        Text(
                                                                "ACCELEROMETER Y",
                                                                color = TextSecondary,
                                                                fontSize = 10.sp
                                                        )
                                                        Spacer(Modifier.height(4.dp))
                                                        Text(
                                                                "%.2f".format(accelY),
                                                                color = TextPrimary
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}

@Composable
fun ActiveSessionScreen(
        timerText: String,
        distanceMeters: Float,
        ax: FloatArray,
        ay: FloatArray,
        az: FloatArray,
        gpsActive: Boolean,
        onStop: () -> Unit,
        cameraPreview: @Composable () -> Unit
) {
        Scaffold(
                containerColor = DarkBg,
                topBar = {
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Icon(
                                        Icons.Default.PlayArrow, // Back placeholder
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
                        // Status Bar
                        Card(
                                colors =
                                        CardDefaults.cardColors(containerColor = Color(0xFF1E2630)),
                                shape = RoundedCornerShape(8.dp),
                                border =
                                        androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                Color(0xFF2C3E50)
                                        ),
                                modifier = Modifier.fillMaxWidth()
                        ) {
                                Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Box(
                                                modifier =
                                                        Modifier.size(10.dp)
                                                                .background(
                                                                        AccentGreen,
                                                                        CircleShape
                                                                )
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
                                                        if (gpsActive) "GPS Locked & Recording"
                                                        else "Waiting for GPS...",
                                                        color = Color(0xFF455A64),
                                                        fontSize = 10.sp
                                                )
                                        }
                                        Button(
                                                onClick = {},
                                                colors =
                                                        ButtonDefaults.buttonColors(
                                                                containerColor = Color(0xFF2C3240)
                                                        ),
                                                contentPadding =
                                                        androidx.compose.foundation.layout
                                                                .PaddingValues(
                                                                        horizontal = 12.dp,
                                                                        vertical = 4.dp
                                                                ),
                                                modifier = Modifier.height(30.dp)
                                        ) { Text("Hide", fontSize = 10.sp, color = TextPrimary) }
                                }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Camera Preview
                        Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(120.dp)
                        ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                        cameraPreview()
                                        Text(
                                                "Visual Context",
                                                color = Color.White.copy(alpha = 0.7f),
                                                fontSize = 10.sp,
                                                modifier =
                                                        Modifier.align(Alignment.BottomEnd)
                                                                .padding(8.dp)
                                        )
                                }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Accelerometer Graph Card
                        Card(
                                colors = CardDefaults.cardColors(containerColor = CardBg),
                                shape = RoundedCornerShape(16.dp),
                                modifier =
                                        Modifier.fillMaxWidth().weight(1f) // Takes available space
                        ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                                Column {
                                                        Text(
                                                                "ACCELEROMETER",
                                                                color = TextSecondary,
                                                                fontSize = 10.sp
                                                        )
                                                        Text(
                                                                "%.2f G-Force".format(
                                                                        az.lastOrNull() ?: 0f
                                                                ),
                                                                color = TextPrimary,
                                                                fontSize = 20.sp,
                                                                fontWeight = FontWeight.Bold
                                                        )
                                                }
                                                Row {
                                                        LegendItem(GraphLineZ, "Z")
                                                        Spacer(Modifier.width(8.dp))
                                                        LegendItem(GraphLineX, "X")
                                                        Spacer(Modifier.width(8.dp))
                                                        LegendItem(GraphLineY, "Y")
                                                }
                                        }

                                        Spacer(Modifier.height(16.dp))

                                        // Graph
                                        Box(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .weight(1f)
                                                                .border(1.dp, Color(0xFF2C3240))
                                        ) { Chart3Lines(ax, ay, az, Modifier.fillMaxSize()) }
                                }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Duration Card
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
                                                Text(
                                                        "DURATION",
                                                        color = TextSecondary,
                                                        fontSize = 10.sp
                                                )
                                                Text(
                                                        timerText,
                                                        color = TextPrimary,
                                                        fontSize = 32.sp,
                                                        fontWeight = FontWeight.Bold
                                                )
                                        }
                                        Box(
                                                modifier =
                                                        Modifier.size(40.dp)
                                                                .background(
                                                                        Color(0xFF2C3E50),
                                                                        CircleShape
                                                                ),
                                                contentAlignment = Alignment.Center
                                        ) { Icon(Icons.Default.History, null, tint = AccentGreen) }
                                }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Stats Row (Distance / Speed)
                        Row(modifier = Modifier.fillMaxWidth()) {
                                Card(
                                        colors = CardDefaults.cardColors(containerColor = CardBg),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.weight(1f)
                                ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Icon(
                                                                Icons.Default.PlayArrow,
                                                                null,
                                                                tint = AccentGreen,
                                                                modifier = Modifier.size(12.dp)
                                                        ) // Distance icon
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(
                                                                "DISTANCE",
                                                                color = TextSecondary,
                                                                fontSize = 10.sp
                                                        )
                                                }
                                                Spacer(Modifier.height(8.dp))
                                                Row(verticalAlignment = Alignment.Bottom) {
                                                        Text(
                                                                "%.1f".format(
                                                                        distanceMeters / 1000f
                                                                ),
                                                                color = TextPrimary,
                                                                fontSize = 20.sp,
                                                                fontWeight = FontWeight.Bold
                                                        )
                                                        Spacer(Modifier.width(4.dp))
                                                        Text(
                                                                "km",
                                                                color = TextSecondary,
                                                                fontSize = 12.sp,
                                                                modifier =
                                                                        Modifier.padding(
                                                                                bottom = 4.dp
                                                                        )
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
                                                Row(
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Icon(
                                                                Icons.Default.PlayArrow,
                                                                null,
                                                                tint = AccentGreen,
                                                                modifier = Modifier.size(12.dp)
                                                        ) // Speed icon
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(
                                                                "AVG SPEED",
                                                                color = TextSecondary,
                                                                fontSize = 10.sp
                                                        )
                                                }
                                                Spacer(Modifier.height(8.dp))
                                                Row(verticalAlignment = Alignment.Bottom) {
                                                        // Simple avg speed calc
                                                        val seconds =
                                                                if (timerText != "00:00") {
                                                                        val parts =
                                                                                timerText.split(":")
                                                                        parts[0].toInt() * 60 +
                                                                                parts[1].toInt()
                                                                } else 1
                                                        val kmh =
                                                                if (seconds > 0)
                                                                        (distanceMeters / 1000f) /
                                                                                (seconds / 3600f)
                                                                else 0f

                                                        Text(
                                                                "%.0f".format(kmh),
                                                                color = TextPrimary,
                                                                fontSize = 20.sp,
                                                                fontWeight = FontWeight.Bold
                                                        )
                                                        Spacer(Modifier.width(4.dp))
                                                        Text(
                                                                "km/h",
                                                                color = TextSecondary,
                                                                fontSize = 12.sp,
                                                                modifier =
                                                                        Modifier.padding(
                                                                                bottom = 4.dp
                                                                        )
                                                        )
                                                }
                                        }
                                }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Stop Button
                        Button(
                                onClick = onStop,
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFC62828)
                                        ), // Red
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

@Composable
fun LegendItem(color: Color, label: String) {
        Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
                Spacer(Modifier.width(4.dp))
                Text(label, color = TextSecondary, fontSize = 10.sp)
        }
}

@Composable
fun Chart3Lines(ax: FloatArray, ay: FloatArray, az: FloatArray, modifier: Modifier) {
        val minV = minOf(ax.minOrNull() ?: -12f, ay.minOrNull() ?: -12f, az.minOrNull() ?: -12f)
        val maxV = maxOf(ax.maxOrNull() ?: 12f, ay.maxOrNull() ?: 12f, az.maxOrNull() ?: 12f)
        val range = (maxV - minV).let { if (it < 1e-3f) 1f else it }

        Canvas(modifier = modifier) {
                // Draw grid
                val midY = size.height / 2
                drawLine(Color(0xFF2C3240), Offset(0f, midY), Offset(size.width, midY))

                fun drawSeries(values: FloatArray, color: Color) {
                        if (values.isEmpty()) return
                        val n = values.size
                        val stepX = if (n > 1) size.width / (n - 1) else size.width
                        val path = Path()
                        for (i in 0 until n) {
                                val x = i * stepX
                                val v = values[i]
                                val y = size.height - ((v - minV) / range) * size.height
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx()))
                }

                drawSeries(az, GraphLineZ)
                drawSeries(ax, GraphLineX)
                drawSeries(ay, GraphLineY)
        }
}

private fun startService(context: Context, action: String) {
        val intent = Intent(context, RecordingService::class.java).apply { this.action = action }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
        } else {
                context.startService(intent)
        }
}
