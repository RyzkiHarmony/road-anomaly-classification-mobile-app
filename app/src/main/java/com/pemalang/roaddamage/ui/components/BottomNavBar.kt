package com.pemalang.roaddamage.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pemalang.roaddamage.ui.theme.md_theme_DarkBg
import com.pemalang.roaddamage.ui.theme.md_theme_CardBg
import com.pemalang.roaddamage.ui.theme.md_theme_AccentGreen
import com.pemalang.roaddamage.ui.theme.md_theme_TextSecondary
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Enum representing the three primary navigation destinations.
 */
enum class NavDestination(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    History("History", Icons.Default.History),
    Settings("Settings", Icons.Default.Settings)
}

// ── Design tokens ──
private val DarkBg = md_theme_DarkBg
private val CardBg = md_theme_CardBg
private val AccentGreen = md_theme_AccentGreen
private val TextSecondary = md_theme_TextSecondary

/**
 * Reusable bottom navigation bar used across all main screens.
 *
 * @param selected    The currently active destination.
 * @param onNavigate  Callback invoked when the user taps a navigation item.
 */
@Composable
fun BottomNavBar(
    selected: NavDestination,
    onNavigate: (NavDestination) -> Unit
) {
    NavigationBar(
        modifier = Modifier.height(80.dp),
        containerColor = DarkBg,
        contentColor = AccentGreen,
        windowInsets = WindowInsets.navigationBars
    ) {
        NavDestination.entries.forEach { dest ->
            NavigationBarItem(
                selected = dest == selected,
                onClick = { if (dest != selected) onNavigate(dest) },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentGreen,
                    selectedTextColor = AccentGreen,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = CardBg
                )
            )
        }
    }
}
