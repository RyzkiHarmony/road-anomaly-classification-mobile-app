package com.pemalang.roaddamage.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.pemalang.roaddamage.ui.theme.md_theme_SurfaceContainerLowest
import com.pemalang.roaddamage.ui.theme.md_theme_OnSurface
import com.pemalang.roaddamage.ui.theme.md_theme_OnSurfaceVariant

// ── Design tokens (Friendly Road Detection) ──
private val DialogBg = md_theme_SurfaceContainerLowest   // white
private val OnSurface = md_theme_OnSurface
private val OnSurfaceVariant = md_theme_OnSurfaceVariant

/**
 * Dialog explaining *why* the app needs certain permissions.
 * Shown when the system indicates that a rationale should be displayed.
 */
@Composable
fun PermissionRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Izin Diperlukan") },
        text = {
            Text(
                "Aplikasi ini membutuhkan akses lokasi dan kamera untuk merekam jejak perjalanan, " +
                        "mendeteksi kerusakan jalan, dan mengambil foto konteks kerusakan. Mohon izinkan akses."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Izinkan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
        containerColor = DialogBg,
        titleContentColor = OnSurface,
        textContentColor = OnSurfaceVariant
    )
}

/**
 * Dialog shown when the user has permanently denied a permission.
 * Offers a shortcut to the system App Settings page.
 */
@Composable
fun PermissionSettingsRedirectDialog(
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Izin Ditolak Permanen") },
        text = {
            Text(
                "Anda telah menolak izin lokasi secara permanen. Mohon aktifkan izin secara manual " +
                        "di Pengaturan Aplikasi untuk menggunakan fitur ini."
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    val intent = Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", ctx.packageName, null)
                    }
                    ctx.startActivity(intent)
                }
            ) { Text("Buka Pengaturan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
        containerColor = DialogBg,
        titleContentColor = OnSurface,
        textContentColor = OnSurfaceVariant
    )
}

/**
 * Dialog informing the user that GPS is disabled and offering to open location settings.
 */
@Composable
fun GpsDisabledDialog(
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GPS Nonaktif") },
        text = {
            Text(
                "Fitur ini memerlukan GPS yang aktif untuk merekam lokasi. Mohon aktifkan GPS Anda."
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    val intent = Intent(AndroidSettings.ACTION_LOCATION_SOURCE_SETTINGS)
                    ctx.startActivity(intent)
                }
            ) { Text("Buka Pengaturan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
        containerColor = DialogBg,
        titleContentColor = OnSurface,
        textContentColor = OnSurfaceVariant
    )
}
