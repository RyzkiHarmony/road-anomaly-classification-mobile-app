package com.pemalang.roaddamage.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.pemalang.roaddamage.model.Trip
import com.pemalang.roaddamage.model.UploadStatus
import com.pemalang.roaddamage.ui.screens.TripListViewModel.SaveEvent
import java.text.SimpleDateFormat
import java.util.*
import com.pemalang.roaddamage.ui.components.BottomNavBar
import com.pemalang.roaddamage.ui.components.NavDestination

import com.pemalang.roaddamage.ui.theme.md_theme_DarkBg
import com.pemalang.roaddamage.ui.theme.md_theme_CardBg
import com.pemalang.roaddamage.ui.theme.md_theme_AccentGreen
import com.pemalang.roaddamage.ui.theme.md_theme_TextPrimary
import com.pemalang.roaddamage.ui.theme.md_theme_TextSecondary
import com.pemalang.roaddamage.ui.theme.md_theme_StatusGreen
import com.pemalang.roaddamage.ui.theme.md_theme_StatusOrange
import com.pemalang.roaddamage.ui.theme.md_theme_StatusRed

private val DarkBg = md_theme_DarkBg
private val CardBg = md_theme_CardBg
private val AccentGreen = md_theme_AccentGreen
private val TextPrimary = md_theme_TextPrimary
private val TextSecondary = md_theme_TextSecondary
private val StatusGreen = md_theme_StatusGreen
private val StatusOrange = md_theme_StatusOrange
private val DeleteRed = md_theme_StatusRed

enum class SortOption {
    NEWEST,
    OLDEST,
    DISTANCE_DESC,
    DURATION_DESC
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListScreen(
        onOpenTrip: (Trip) -> Unit,
        onNavigateHome: () -> Unit,
        onNavigateSettings: () -> Unit
) {
    val vm: TripListViewModel = hiltViewModel()
    val trips by vm.trips.collectAsState()
    val host = remember { SnackbarHostState() }
    val ctx = LocalContext.current

    var filter by remember { mutableStateOf("All") } // All, Uploaded, Pending
    var sortOption by remember { mutableStateOf(SortOption.NEWEST) }
    var showSortMenu by remember { mutableStateOf(false) }
    var tripToDelete by remember { mutableStateOf<Trip?>(null) }

    if (tripToDelete != null) {
        AlertDialog(
                onDismissRequest = { tripToDelete = null },
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
                                vm.deleteTrip(tripToDelete!!)
                                tripToDelete = null
                            }
                    ) { Text("Hapus", color = DeleteRed, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { tripToDelete = null }) {
                        Text("Batal", color = TextSecondary)
                    }
                },
                containerColor = CardBg,
                textContentColor = TextSecondary,
                titleContentColor = TextPrimary
        )
    }

    val filteredTrips =
            remember(trips, filter, sortOption) {
                val list =
                        when (filter) {
                            "Uploaded" -> trips.filter { it.uploadStatus == UploadStatus.UPLOADED }
                            "Pending" -> trips.filter { it.uploadStatus != UploadStatus.UPLOADED }
                            else -> trips
                        }
                when (sortOption) {
                    SortOption.NEWEST -> list.sortedByDescending { it.startTime }
                    SortOption.OLDEST -> list.sortedBy { it.startTime }
                    SortOption.DISTANCE_DESC -> list.sortedByDescending { it.distance }
                    SortOption.DURATION_DESC -> list.sortedByDescending { it.duration }
                }
            }

    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            when (e) {
                is SaveEvent.Success -> {
                    val msg = if (e.uri != null) "Tersimpan di Unduhan" else "Tersimpan di Lokal"
                    val res = host.showSnackbar(message = msg, actionLabel = "Buka")
                    if (res == SnackbarResult.ActionPerformed) {
                        if (e.uri != null) openCsv(ctx, e.uri)
                        else if (e.path != null) openLocalFile(ctx, e.path)
                    }
                }
                is SaveEvent.Error -> {
                    host.showSnackbar(e.message)
                }
            }
        }
    }

    Scaffold(
            containerColor = DarkBg,
            snackbarHost = { SnackbarHost(hostState = host) },
            bottomBar = {
                BottomNavBar(
                    selected = NavDestination.History,
                    onNavigate = { dest ->
                        when(dest) {
                            NavDestination.Home -> onNavigateHome()
                            NavDestination.Settings -> onNavigateSettings()
                            else -> { /* already on History */ }
                        }
                    }
                )
            }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                    text = "LOG DATA",
                    color = AccentGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
            )
            Text(
                    text = "Riwayat Perjalanan",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Filters & Sort
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterButton("Semua", filter == "All") { filter = "All" }
                    FilterButton("Terunggah", filter == "Uploaded") { filter = "Uploaded" }
                    FilterButton("Tertunda", filter == "Pending") { filter = "Pending" }
                }

                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                                Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Urutkan",
                                tint = AccentGreen
                        )
                    }
                    DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            containerColor = CardBg
                    ) {
                        DropdownMenuItem(
                                text = { Text("Terbaru", color = TextPrimary) },
                                onClick = {
                                    sortOption = SortOption.NEWEST
                                    showSortMenu = false
                                }
                        )
                        DropdownMenuItem(
                                text = { Text("Terlama", color = TextPrimary) },
                                onClick = {
                                    sortOption = SortOption.OLDEST
                                    showSortMenu = false
                                }
                        )
                        DropdownMenuItem(
                                text = { Text("Jarak Terjauh", color = TextPrimary) },
                                onClick = {
                                    sortOption = SortOption.DISTANCE_DESC
                                    showSortMenu = false
                                }
                        )
                        DropdownMenuItem(
                                text = { Text("Durasi Terlama", color = TextPrimary) },
                                onClick = {
                                    sortOption = SortOption.DURATION_DESC
                                    showSortMenu = false
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredTrips.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                                Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                                text = "Belum ada perjalanan",
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                        )
                        Text(
                                text = "Mulai rekam perjalanan Anda sekarang!",
                                color = TextSecondary.copy(alpha = 0.7f),
                                fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTrips, key = { it.tripId }) { trip ->
                        @Suppress("DEPRECATION")
                        val dismissState = rememberSwipeToDismissBoxState()
                        
                        LaunchedEffect(dismissState.currentValue) {
                            if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                                tripToDelete = trip
                                dismissState.reset()
                            }
                        }

                        SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    val color = DeleteRed
                                    Box(
                                            modifier =
                                                    Modifier.fillMaxSize()
                                                            .background(
                                                                    color,
                                                                    RoundedCornerShape(16.dp)
                                                            )
                                                            .padding(horizontal = 20.dp),
                                            contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color.White
                                        )
                                    }
                                },
                                content = { TripCard(trip = trip, onClick = { onOpenTrip(trip) }) },
                                enableDismissFromStartToEnd = false,
                                enableDismissFromEndToStart = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Button(
            onClick = onClick,
            colors =
                    ButtonDefaults.buttonColors(
                            containerColor = if (selected) AccentGreen else CardBg,
                            contentColor = if (selected) Color.Black else TextSecondary
                    ),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.height(36.dp)
    ) { Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun TripCard(trip: Trip, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

    val dateStr = dateFormat.format(Date(trip.startTime))
    val timeStr = timeFormat.format(Date(trip.startTime))
    val dayStr = dayFormat.format(Date(trip.startTime)).uppercase()

    val isUploaded = trip.uploadStatus == UploadStatus.UPLOADED

    Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Date & Status
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                            modifier =
                                    Modifier.size(40.dp)
                                            .background(
                                                    Color(0xFF2C3E50),
                                                    RoundedCornerShape(8.dp)
                                            ),
                            contentAlignment = Alignment.Center
                    ) {
                        Icon(
                                Icons.Default.DateRange,
                                null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                                text = dateStr,
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                        )
                        Text(text = "$timeStr • $dayStr", color = TextSecondary, fontSize = 12.sp)
                    }
                }

                // Status Badge
                Surface(
                        color = (if (isUploaded) StatusGreen else StatusOrange).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                                modifier =
                                        Modifier.size(6.dp)
                                                .background(
                                                        if (isUploaded) StatusGreen
                                                        else StatusOrange,
                                                        CircleShape
                                                )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                                text = if (isUploaded) "UPLOADED" else "PENDING",
                                color = if (isUploaded) StatusGreen else StatusOrange,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF2C3E50), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Stats
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "DISTANCE", color = TextSecondary, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                                text = "%.1f".format(trip.distance / 1000f),
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                        )
                        Text(
                                " km",
                                color = AccentGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "DURATION", color = TextSecondary, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    val m = (trip.duration / 60).toInt()
                    val s = (trip.duration % 60).toInt()
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                                text = "$m",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                        )
                        Text(
                                " m ",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 3.dp)
                        )
                        Text(
                                text = "$s",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                        )
                        Text(
                                " s",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun openCsv(context: Context, uri: Uri) {
    val intent =
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/csv")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
    context.startActivity(intent)
}

private fun openLocalFile(context: Context, path: String) {
    val fileUri =
            FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    java.io.File(path)
            )
    val intent =
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "text/csv")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
    context.startActivity(intent)
}
