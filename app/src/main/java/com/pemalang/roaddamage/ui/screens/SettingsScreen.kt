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

import com.pemalang.roaddamage.ui.theme.md_theme_DarkBg
import com.pemalang.roaddamage.ui.theme.md_theme_CardBg
import com.pemalang.roaddamage.ui.theme.md_theme_AccentGreen
import com.pemalang.roaddamage.ui.theme.md_theme_TextPrimary
import com.pemalang.roaddamage.ui.theme.md_theme_TextSecondary

private val DarkBg = md_theme_DarkBg
private val CardBg = md_theme_CardBg
private val AccentGreen = md_theme_AccentGreen
private val TextPrimary = md_theme_TextPrimary
private val TextSecondary = md_theme_TextSecondary
private val DeleteRed = Color(0xFFEF5350)
private val StatusGreen = Color(0xFF00E676)

@Composable
fun SettingsScreen(onBack: () -> Unit, onNavigateHome: () -> Unit, onNavigateTrips: () -> Unit) {
        val vm: SettingsViewModel = hiltViewModel()
        val ui by vm.ui.collectAsState()
        val host = remember { SnackbarHostState() }
        val scrollState = rememberScrollState()

        // Temporary local state for sliders
        var sliderHz by remember(ui.samplingHz) { mutableStateOf(ui.samplingHz.toFloat()) }
        var sliderGps by remember(ui.gpsIntervalSec) { mutableStateOf(ui.gpsIntervalSec.toFloat()) }
        var sliderSensitivity by
                remember(ui.sensitivityThreshold) { mutableStateOf(ui.sensitivityThreshold) }

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
                containerColor = DarkBg,
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
                                                tint = TextPrimary
                                        )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                        text = "KONFIGURASI",
                                        color = TextPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
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
                                        value = "${sliderHz.toInt()} Hz"
                                ) {
                                        Slider(
                                                value = sliderHz,
                                                onValueChange = { sliderHz = it },
                                                onValueChangeFinished = {
                                                        vm.setSampling(sliderHz.toInt())
                                                },
                                                valueRange = 100f..100f,
                                                steps = 0,
                                                enabled = false, // DISABLED: Changing this breaks the ML Pipeline (Butterworth Filter & ONNX Tensor size)
                                                // increments roughly
                                                colors =
                                                        SliderDefaults.colors(
                                                                thumbColor = AccentGreen,
                                                                activeTrackColor = AccentGreen,
                                                                inactiveTrackColor = CardBg
                                                        )
                                        )
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                                Text("10", color = TextSecondary, fontSize = 10.sp)
                                                Text("50", color = TextSecondary, fontSize = 10.sp)
                                                Text("100", color = TextSecondary, fontSize = 10.sp)
                                        }
                                }

                                // GPS Interval
                                ParameterCard(
                                        title = "Interval GPS",
                                        subtitle = "Frekuensi pembaruan lokasi",
                                        value = "${sliderGps.toInt()} s"
                                ) {
                                        Slider(
                                                value = sliderGps,
                                                onValueChange = { sliderGps = it },
                                                onValueChangeFinished = {
                                                        vm.setGpsInterval(sliderGps.toInt())
                                                },
                                                valueRange = 1f..20f,
                                                colors =
                                                        SliderDefaults.colors(
                                                                thumbColor = AccentGreen,
                                                                activeTrackColor = AccentGreen,
                                                                inactiveTrackColor = CardBg
                                                        )
                                        )
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                                Text("1s", color = TextSecondary, fontSize = 10.sp)
                                                Text("10s", color = TextSecondary, fontSize = 10.sp)
                                                Text("20s", color = TextSecondary, fontSize = 10.sp)
                                        }
                                }

                                // Sensitivity Threshold / Camera Trigger
                                ParameterCard(
                                        title = "Trigger Kamera (G-Force)",
                                        subtitle = "Minimal guncangan untuk ambil foto",
                                        value = String.format("%.1f G", sliderSensitivity)
                                ) {
                                        // Slider
                                        Slider(
                                                value = sliderSensitivity,
                                                onValueChange = { sliderSensitivity = it },
                                                onValueChangeFinished = {
                                                        vm.setSensitivity(sliderSensitivity)
                                                },
                                                valueRange = 1.4f..20.0f,
                                                steps = 186, // (20.0 - 1.4) / 0.1 ~= 186 steps
                                                colors =
                                                        SliderDefaults.colors(
                                                                thumbColor = AccentGreen,
                                                                activeTrackColor = AccentGreen,
                                                                inactiveTrackColor = CardBg
                                                        )
                                        )

                                        // Manual Input Column
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                                Text(
                                                        "Min: 1.4G",
                                                        color = TextSecondary,
                                                        fontSize = 10.sp
                                                )

                                                // Local state for text input to allow intermediate values
                                                var textValue by remember { mutableStateOf(String.format(java.util.Locale.US, "%.1f", sliderSensitivity)) }

                                                // Sync textValue when sliderSensitivity changes externally
                                                LaunchedEffect(sliderSensitivity) {
                                                    val parsed = textValue.toFloatOrNull()
                                                    if (parsed == null || kotlin.math.abs(parsed - sliderSensitivity) > 0.05f) {
                                                        textValue = String.format(java.util.Locale.US, "%.1f", sliderSensitivity)
                                                    }
                                                }

                                                // Input Field
                                                androidx.compose.material3.OutlinedTextField(
                                                        value = textValue,
                                                        onValueChange = { str ->
                                                                textValue = str
                                                                val num = str.toFloatOrNull()
                                                                if (num != null && num in 1.4f..20.0f) {
                                                                        sliderSensitivity = num
                                                                        vm.setSensitivity(num)
                                                                }
                                                        },
                                                        label = {
                                                                Text("Input G", fontSize = 10.sp)
                                                        },
                                                        singleLine = true,
                                                        keyboardOptions =
                                                                androidx.compose.foundation.text
                                                                        .KeyboardOptions(
                                                                                keyboardType =
                                                                                        androidx.compose
                                                                                                .ui
                                                                                                .text
                                                                                                .input
                                                                                                .KeyboardType
                                                                                                .Decimal
                                                                        ),
                                                        modifier =
                                                                Modifier.width(80.dp).height(56.dp),
                                                        textStyle =
                                                                androidx.compose.ui.text.TextStyle(
                                                                        fontSize = 12.sp,
                                                                        color = TextPrimary
                                                                ),
                                                        colors =
                                                                androidx.compose.material3
                                                                        .OutlinedTextFieldDefaults
                                                                        .colors(
                                                                                focusedBorderColor =
                                                                                        AccentGreen,
                                                                                unfocusedBorderColor =
                                                                                        TextSecondary,
                                                                                focusedLabelColor =
                                                                                        AccentGreen,
                                                                                unfocusedLabelColor =
                                                                                        TextSecondary
                                                                        )
                                                )

                                                Text(
                                                        "Max: 20G",
                                                        color = TextSecondary,
                                                        fontSize = 10.sp
                                                )
                                        }
                                }
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
                                                                                        Color(
                                                                                                0xFF2C3E50
                                                                                        ),
                                                                                        RoundedCornerShape(
                                                                                                8.dp
                                                                                        )
                                                                                ),
                                                                contentAlignment = Alignment.Center
                                                        ) {
                                                                Icon(
                                                                        Icons.Default.Wifi,
                                                                        null,
                                                                        tint = TextSecondary
                                                                )
                                                        }
                                                        Spacer(modifier = Modifier.width(16.dp))
                                                        Column {
                                                                Text(
                                                                        "Unggah Otomatis di WiFi",
                                                                        color = TextPrimary,
                                                                        fontWeight =
                                                                                FontWeight.Bold,
                                                                        fontSize = 16.sp
                                                                )
                                                                Text(
                                                                        "Hemat penggunaan data seluler",
                                                                        color = TextSecondary,
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
                                                                                AccentGreen,
                                                                        checkedTrackColor =
                                                                                Color(0xFF004D40),
                                                                        uncheckedThumbColor =
                                                                                TextSecondary,
                                                                        uncheckedTrackColor = DarkBg
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
                                                        fontWeight = FontWeight.Bold
                                                )
                                        }
                                }

                                Text(
                                        "Hanya menghapus salinan lokal dari data yang sudah disinkronkan ke cloud.",
                                        color = TextSecondary,
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
                                                        color = TextPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                        "Aplikasi ini mendeteksi dan memetakan kerusakan jalan secara otomatis menggunakan sensor smartphone. Data yang dikumpulkan membantu pemantauan infrastruktur.",
                                                        color = TextSecondary,
                                                        fontSize = 12.sp,
                                                        lineHeight = 18.sp
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))
                                                HorizontalDivider(color = DarkBg, thickness = 1.dp)
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Row(
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                ) {
                                                        Text(
                                                                "Versi Aplikasi",
                                                                color = TextSecondary,
                                                                fontSize = 12.sp
                                                        )
                                                        Text(
                                                                "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                                                color = TextPrimary,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold
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
                                                        .background(Color(0xFF2C3E50))
                                                        .padding(8.dp),
                                        colorFilter =
                                                androidx.compose.ui.graphics.ColorFilter.tint(
                                                        TextSecondary
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
                                        color = TextPrimary,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                )
                                Text(
                                        text = "ID: #$id",
                                        color = AccentGreen,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                        color = Color(0xFF1A3F45),
                                        shape = RoundedCornerShape(4.dp)
                                ) {
                                        Text(
                                                text = "• $role",
                                                color = AccentGreen,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
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
                title = { Text("Edit Profile", color = TextPrimary) },
                text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        label = { Text("Name") },
                                        colors =
                                                OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = TextPrimary,
                                                        unfocusedTextColor = TextPrimary,
                                                        focusedBorderColor = AccentGreen,
                                                        unfocusedBorderColor = TextSecondary,
                                                        focusedLabelColor = AccentGreen,
                                                        unfocusedLabelColor = TextSecondary
                                                )
                                )
                                OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        label = { Text("Email") },
                                        colors =
                                                OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = TextPrimary,
                                                        unfocusedTextColor = TextPrimary,
                                                        focusedBorderColor = AccentGreen,
                                                        unfocusedBorderColor = TextSecondary,
                                                        focusedLabelColor = AccentGreen,
                                                        unfocusedLabelColor = TextSecondary
                                                )
                                )
                                OutlinedTextField(
                                        value = vehicle,
                                        onValueChange = { vehicle = it },
                                        label = { Text("Vehicle Type (e.g., Motor, Mobil)") },
                                        colors =
                                                OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = TextPrimary,
                                                        unfocusedTextColor = TextPrimary,
                                                        focusedBorderColor = AccentGreen,
                                                        unfocusedBorderColor = TextSecondary,
                                                        focusedLabelColor = AccentGreen,
                                                        unfocusedLabelColor = TextSecondary
                                                )
                                )
                        }
                },
                confirmButton = {
                        Button(
                                onClick = { onSave(name, email, vehicle) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                        ) { Text("Save", color = Color.Black) }
                },
                dismissButton = {
                        TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
                }
        )
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
        Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                        icon,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                        text = title,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                )
        }
}

@Composable
fun ParameterCard(
        title: String,
        subtitle: String,
        value: String,
        content: @Composable ColumnScope.() -> Unit
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
                                                color = TextPrimary,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                                text = subtitle,
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                        )
                                }
                                Surface(
                                        color = AccentGreen.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                ) {
                                        Text(
                                                text = value,
                                                color = AccentGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier =
                                                        Modifier.padding(
                                                                horizontal = 8.dp,
                                                                vertical = 4.dp
                                                        )
                                        )
                                }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        content()
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
