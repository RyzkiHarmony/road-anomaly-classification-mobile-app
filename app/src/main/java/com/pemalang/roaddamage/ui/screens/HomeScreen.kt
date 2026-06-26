package com.pemalang.roaddamage.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pemalang.roaddamage.recording.RecordingService
import com.pemalang.roaddamage.ui.components.GpsDisabledDialog
import com.pemalang.roaddamage.ui.components.PermissionRationaleDialog
import com.pemalang.roaddamage.ui.components.PermissionSettingsRedirectDialog
import java.io.File
import kotlinx.coroutines.delay

import com.pemalang.roaddamage.ui.theme.md_theme_Primary
import com.pemalang.roaddamage.ui.theme.md_theme_OnSurfaceVariant

// ── Design tokens (Friendly Road Detection) ──
private val AccentGreen = md_theme_Primary
private val TextSecondary = md_theme_OnSurfaceVariant

/**
 * Top-level screen that orchestrates:
 *  – permission requests & dialogs (via [PermissionRationaleDialog] etc.)
 *  – delegation to [DashboardScreen] (idle) or [ActiveSessionScreen] (recording)
 *
 * After refactoring this file shrunk from ~1 200 → ~250 lines.
 */
@Composable
fun HomeScreen(
    onStartRecording: () -> Unit,
    onOpenTrips: () -> Unit,
    onOpenSettings: () -> Unit = {},
    vm: RecordingViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current

    // ── Collect ViewModel state ──
    val isRecording by vm.recording.collectAsState()
    val startTime by vm.startTime.collectAsState()
    val distance by vm.distance.collectAsState()
    val ax by vm.ax.collectAsState()
    val ay by vm.ay.collectAsState()
    val az by vm.az.collectAsState()
    val gpsActive by vm.gpsActive.collectAsState()
    val isGpsEnabled by vm.isGpsEnabled.collectAsState()
    val totalTrips by vm.totalTrips.collectAsState()
    val totalDistance by vm.totalDistance.collectAsState()
    val samplingRate by vm.samplingRate.collectAsState()
    val eventCount by vm.eventCount.collectAsState()
    val currentSpeedKmh by vm.currentSpeedKmh.collectAsState()
    val userName by vm.userName.collectAsState()
    val pendingUploads by vm.pendingUploads.collectAsState()
    val anomalyProbabilities by vm.anomalyProbabilities.collectAsState()
    // ── Permission state ──
    var showRationale by remember { mutableStateOf(false) }
    var showSettingsRedirect by remember { mutableStateOf(false) }
    var showGpsDialog by remember { mutableStateOf(false) }

    val requiredPermissions = remember {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= 33) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        perms.toTypedArray()
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { res ->
        val locGranted = res[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                res[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val notifGranted = Build.VERSION.SDK_INT < 33 ||
                res[Manifest.permission.POST_NOTIFICATIONS] == true

        if (locGranted && notifGranted) {
            startService(ctx, RecordingService.ACTION_START)
        } else {
            val activity = ctx as? Activity
            val shouldShow = requiredPermissions.any {
                activity?.shouldShowRequestPermissionRationale(it) == true
            }
            if (!shouldShow) showSettingsRedirect = true
        }
    }

    // ── Permission Dialogs ──
    if (showRationale) {
        PermissionRationaleDialog(
            onConfirm = {
                showRationale = false
                launcher.launch(requiredPermissions)
            },
            onDismiss = { showRationale = false }
        )
    }
    if (showSettingsRedirect) {
        PermissionSettingsRedirectDialog(onDismiss = { showSettingsRedirect = false })
    }
    if (showGpsDialog) {
        GpsDisabledDialog(onDismiss = { showGpsDialog = false })
    }

    // ── Timer text ──
    val timerText = remember { mutableStateOf("00:00") }
    LaunchedEffect(isRecording) {
        while (isRecording) {
            val start = startTime
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
        if (!isRecording) timerText.value = "00:00"
    }
    // ── Screen delegation ──
    if (!isRecording) {
        DashboardScreen(
            onStartRecording = {
                val allGranted = requiredPermissions.all {
                    ContextCompat.checkSelfPermission(ctx, it) ==
                            PackageManager.PERMISSION_GRANTED
                }
                if (allGranted) {
                    val lm = ctx.getSystemService(Context.LOCATION_SERVICE)
                            as android.location.LocationManager
                    if (!lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                        showGpsDialog = true
                    } else {
                        startService(ctx, RecordingService.ACTION_START)
                    }
                } else {
                    val activity = ctx as? Activity
                    val shouldShow = requiredPermissions.any {
                        activity?.shouldShowRequestPermissionRationale(it) == true
                    }
                    if (shouldShow) showRationale = true
                    else launcher.launch(requiredPermissions)
                }
            },
            onOpenTrips = onOpenTrips,
            onOpenSettings = onOpenSettings,
            totalTrips = totalTrips,
            totalDist = totalDistance ?: 0f,
            accelX = ax.lastOrNull() ?: 0f,
            accelY = ay.lastOrNull() ?: 0f,
            samplingRate = samplingRate,
            eventCount = eventCount,
            userName = userName,
            pendingUploads = pendingUploads,
            isGpsEnabled = isGpsEnabled
        )
    } else {
        ActiveSessionScreen(
            timerText = timerText.value,
            distanceMeters = distance,
            ax = ax,
            ay = ay,
            az = az,
            gpsActive = gpsActive,
            currentSpeedKmh = currentSpeedKmh,
            onStop = { startService(ctx, RecordingService.ACTION_STOP) },
            anomalyProbabilities = anomalyProbabilities
        )
    }
}

/**
 * Helper to start / stop the [RecordingService] foreground service.
 */
private fun startService(context: Context, action: String) {
    val intent = Intent(context, RecordingService::class.java).apply { this.action = action }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}
