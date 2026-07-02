package com.pemalang.roaddamage.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pemalang.roaddamage.BuildConfig
import com.pemalang.roaddamage.ui.components.BottomNavBar
import com.pemalang.roaddamage.ui.components.NavDestination

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
private val InputBg = md_theme_InputBg
private val DeleteRed = md_theme_Error
private val StatusGreen = md_theme_Primary

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateTrips: () -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
        val ui by vm.ui.collectAsState()
        val host = remember { SnackbarHostState() }
        val scrollState = rememberScrollState()

        // Edit Profile Dialog State
        var showEditProfile by remember { mutableStateOf(false) }

        if (showEditProfile) {
                EditProfileDialog(
                        currentName = ui.userName,
                        currentEmail = ui.userEmail,
                        currentVehicle = ui.vehicleType,
                        onDismiss = { showEditProfile = false },
                        onSave = { name, email, vehicle ->
                                vm.setUserName(name)
                                vm.setUserEmail(email)
                                vm.setVehicleType(vehicle)
                                showEditProfile = false
                        }
                )
        }

        Scaffold(
                containerColor = SurfaceBg,
                snackbarHost = { SnackbarHost(hostState = host) },
                topBar = {
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                IconButton(onClick = onBack) {
                                        Icon(
                                                Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back",
                                                tint = OnSurface
                                        )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                        text = "KONFIGURASI",
                                        color = OnSurface,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Spacer(modifier = Modifier.width(48.dp)) // Balance the back button
                        }
                },
                bottomBar = {
                        BottomNavBar(
                                selected = NavDestination.Settings,
                                onNavigate = { dest ->
                                        when (dest) {
                                                NavDestination.Home -> onNavigateHome()
                                                NavDestination.History -> onNavigateTrips()
                                                else -> { /* already on Settings */ }
                                        }
                                }
                        )
                }
        ) { padding ->
                Column(
                        modifier =
                                Modifier.fillMaxSize()
                                        .padding(padding)
                                        .verticalScroll(scrollState)
                                        .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                        // User Profile Card
                        UserProfileCard(
                                name = ui.userName.ifEmpty { "User" },
                                id = ui.userId.take(8).uppercase(), // Shorten ID for display
                                role = ui.vehicleType.ifEmpty { "Unknown Vehicle" },
                                onClick = { showEditProfile = true }
                        )

                        // Sensor Parameters Section
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                SectionHeader("SENSOR PARAMETERS", Icons.Default.Sensors)

                                // Accelerometer Rate
                                ParameterCard(
                                        title = "Accelerometer Rate",
                                        subtitle = "Sampling frequency in Hz",
                                        value = "${ui.samplingHz} Hz"
                                )

                                // GPS Interval
                                ParameterCard(
                                        title = "Interval GPS",
                                        subtitle = "Frekuensi pembaruan lokasi",
                                        value = "${ui.gpsIntervalSec} s"
                                )


                        }

                        // Data Management Section
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                SectionHeader("MANAJEMEN DATA", Icons.Default.Storage)

                                // Auto-upload
                                Card(
                                        colors = CardDefaults.cardColors(containerColor = CardBg),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                ) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Row(
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Box(
                                                                modifier =
                                                                        Modifier.size(40.dp)
                                                                                .background(
                                                                                        SurfaceContainer,
                                                                                        RoundedCornerShape(
                                                                                                8.dp
                                                                                        )
                                                                                ),
                                                                contentAlignment = Alignment.Center
                                                        ) {
                                                                Icon(
                                                                        Icons.Default.Wifi,
                                                                        null,
                                                                        tint = OnSurfaceVariant
                                                                )
                                                        }
                                                        Spacer(modifier = Modifier.width(16.dp))
                                                        Column {
                                                                Text(
                                                                        "Unggah Otomatis di WiFi",
                                                                        color = OnSurface,
                                                                        fontWeight =
                                                                                FontWeight.SemiBold,
                                                                        fontSize = 16.sp
                                                                )
                                                                Text(
                                                                        "Hemat penggunaan data seluler",
                                                                        color = OnSurfaceVariant,
                                                                        fontSize = 12.sp
                                                                )
                                                        }
                                                }
                                                Switch(
                                                        checked = ui.autoUpload,
                                                        onCheckedChange = { vm.setAutoUpload(it) },
                                                        colors =
                                                                SwitchDefaults.colors(
                                                                        checkedThumbColor =
                                                                                Primary,
                                                                        checkedTrackColor =
                                                                                PrimaryLight.copy(alpha = 0.5f),
                                                                        uncheckedThumbColor =
                                                                                OnSurfaceVariant,
                                                                        uncheckedTrackColor = SurfaceContainer
                                                                )
                                                )
                                        }
                                }

                                // Clear Data Button
                                Button(
                                        onClick = { vm.deleteUploadedTrips() },
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor = Color.Transparent
                                                ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .border(
                                                                1.dp,
                                                                DeleteRed.copy(alpha = 0.5f),
                                                                RoundedCornerShape(12.dp)
                                                        )
                                                        .height(56.dp)
                                ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Delete, null, tint = DeleteRed)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                        "Hapus Data Terunggah",
                                                        color = DeleteRed,
                                                        fontWeight = FontWeight.SemiBold
                                                )
                                        }
                                }

                                Text(
                                        "Hanya menghapus salinan lokal dari data yang sudah disinkronkan ke cloud.",
                                        color = OnSurfaceVariant,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // About Section
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                SectionHeader("TENTANG APLIKASI", Icons.Default.Info)

                                Card(
                                        colors = CardDefaults.cardColors(containerColor = CardBg),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                ) {
                                        Column(modifier = Modifier.padding(20.dp)) {
                                                Text(
                                                        "Road Damage Detector",
                                                        color = OnSurface,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 16.sp
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                        "Aplikasi ini mendeteksi dan memetakan kerusakan jalan secara otomatis menggunakan sensor smartphone. Data yang dikumpulkan membantu pemantauan infrastruktur.",
                                                        color = OnSurfaceVariant,
                                                        fontSize = 12.sp,
                                                        lineHeight = 18.sp
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))
                                                HorizontalDivider(color = OutlineVar.copy(alpha = 0.5f), thickness = 1.dp)
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Row(
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                ) {
                                                        Text(
                                                                "Versi Aplikasi",
                                                                color = OnSurfaceVariant,
                                                                fontSize = 12.sp
                                                        )
                                                        Text(
                                                                "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                                                color = OnSurface,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                        )
                                                }
                                        }
                                }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                }
        }
}

@Composable
fun UserProfileCard(name: String, id: String, role: String, onClick: () -> Unit) {
        Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().clickable { onClick() }
        ) {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Box {
                                Image(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier =
                                                Modifier.size(60.dp)
                                                        .clip(CircleShape)
                                                        .background(SurfaceContainer)
                                                        .padding(8.dp),
                                        colorFilter =
                                                androidx.compose.ui.graphics.ColorFilter.tint(
                                                        OnSurfaceVariant
                                                )
                                )
                                Box(
                                        modifier =
                                                Modifier.size(16.dp)
                                                        .background(StatusGreen, CircleShape)
                                                        .border(2.dp, CardBg, CircleShape)
                                                        .align(Alignment.BottomEnd)
                                )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                                Text(
                                        text = name,
                                        color = OnSurface,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                        text = "ID: #$id",
                                        color = Primary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                        color = PrimaryLight.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(4.dp)
                                ) {
                                        Text(
                                                text = "• $role",
                                                color = Primary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier =
                                                        Modifier.padding(
                                                                horizontal = 8.dp,
                                                                vertical = 4.dp
                                                        )
                                        )
                                }
                        }
                }
        }
}

@Composable
fun EditProfileDialog(
        currentName: String,
        currentEmail: String,
        currentVehicle: String,
        onDismiss: () -> Unit,
        onSave: (String, String, String) -> Unit
) {
        var name by remember { mutableStateOf(currentName) }
        var email by remember { mutableStateOf(currentEmail) }
        var vehicle by remember { mutableStateOf(currentVehicle) }

        AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = CardBg,
                title = { Text("Edit Profile", color = OnSurface) },
                text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        label = { Text("Name") },
                                        colors =
                                                OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = OnSurface,
                                                        unfocusedTextColor = OnSurface,
                                                        focusedBorderColor = Primary,
                                                        unfocusedBorderColor = OutlineVar,
                                                        focusedLabelColor = Primary,
                                                        unfocusedLabelColor = OnSurfaceVariant,
                                                        focusedContainerColor = InputBg,
                                                        unfocusedContainerColor = InputBg
                                                )
                                )
                                OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        label = { Text("Email") },
                                        colors =
                                                OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = OnSurface,
                                                        unfocusedTextColor = OnSurface,
                                                        focusedBorderColor = Primary,
                                                        unfocusedBorderColor = OutlineVar,
                                                        focusedLabelColor = Primary,
                                                        unfocusedLabelColor = OnSurfaceVariant,
                                                        focusedContainerColor = InputBg,
                                                        unfocusedContainerColor = InputBg
                                                )
                                )
                                OutlinedTextField(
                                        value = vehicle,
                                        onValueChange = { vehicle = it },
                                        label = { Text("Vehicle Type (e.g., Motor, Mobil)") },
                                        colors =
                                                OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = OnSurface,
                                                        unfocusedTextColor = OnSurface,
                                                        focusedBorderColor = Primary,
                                                        unfocusedBorderColor = OutlineVar,
                                                        focusedLabelColor = Primary,
                                                        unfocusedLabelColor = OnSurfaceVariant,
                                                        focusedContainerColor = InputBg,
                                                        unfocusedContainerColor = InputBg
                                                )
                                )
                        }
                },
                confirmButton = {
                        Button(
                                onClick = { onSave(name, email, vehicle) },
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) { Text("Save", color = Color(0xFFFAFAFA)) }
                },
                dismissButton = {
                        TextButton(onClick = onDismiss) { Text("Cancel", color = OnSurfaceVariant) }
                }
        )
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
        Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                        icon,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                        text = title,
                        color = OnSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                )
        }
}

@Composable
fun ParameterCard(
        title: String,
        subtitle: String,
        value: String,
        content: (@Composable ColumnScope.() -> Unit)? = null
) {
        Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
        ) {
                Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                        ) {
                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text = title,
                                                color = OnSurface,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                                text = subtitle,
                                                color = OnSurfaceVariant,
                                                fontSize = 12.sp
                                        )
                                }
                                Surface(
                                        color = PrimaryLight.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(8.dp)
                                ) {
                                        Text(
                                                text = value,
                                                color = Primary,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                modifier =
                                                        Modifier.padding(
                                                                horizontal = 8.dp,
                                                                vertical = 4.dp
                                                        )
                                        )
                                }
                        }
                        if (content != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                content()
                        }
                }
        }
}

@Composable
fun Image(
        imageVector: ImageVector,
        contentDescription: String?,
        modifier: Modifier = Modifier,
        colorFilter: androidx.compose.ui.graphics.ColorFilter? = null
) {
        androidx.compose.foundation.Image(
                imageVector = imageVector,
                contentDescription = contentDescription,
                modifier = modifier,
                colorFilter = colorFilter
        )
}
