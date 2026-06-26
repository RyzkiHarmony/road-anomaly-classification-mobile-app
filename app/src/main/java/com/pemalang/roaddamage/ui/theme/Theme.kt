package com.pemalang.roaddamage.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable


// ── Surface & Background ──
val md_theme_Surface         = Color(0xFFF8F9FA)
val md_theme_SurfaceDim      = Color(0xFFD9DADB)
val md_theme_SurfaceBright   = Color(0xFFF8F9FA)
val md_theme_SurfaceContainerLowest = Color(0xFFFFFFFF)
val md_theme_SurfaceContainerLow    = Color(0xFFF3F4F5)
val md_theme_SurfaceContainer       = Color(0xFFEDEEEF)
val md_theme_SurfaceContainerHigh   = Color(0xFFE7E8E9)
val md_theme_SurfaceContainerHighest = Color(0xFFE1E3E4)
val md_theme_SurfaceVariant  = Color(0xFFE1E3E4)

// ── On Surface (Text) ──
val md_theme_OnSurface        = Color(0xFF191C1D)
val md_theme_OnSurfaceVariant = Color(0xFF404943)
val md_theme_InverseSurface   = Color(0xFF2E3132)
val md_theme_InverseOnSurface = Color(0xFFF0F1F2)

// ── Primary (Forest Green / Emerald) ──
val md_theme_Primary            = Color(0xFF0F5238)
val md_theme_OnPrimary          = Color(0xFFFFFFFF)
val md_theme_PrimaryContainer   = Color(0xFF2D6A4F)
val md_theme_OnPrimaryContainer = Color(0xFFA8E7C5)
val md_theme_InversePrimary     = Color(0xFF95D4B3)
val md_theme_PrimaryFixed       = Color(0xFFB1F0CE)
val md_theme_PrimaryFixedDim    = Color(0xFF95D4B3)
val md_theme_SurfaceTint        = Color(0xFF2C694E)

// ── Secondary ──
val md_theme_Secondary            = Color(0xFF3E6750)
val md_theme_OnSecondary          = Color(0xFFFFFFFF)
val md_theme_SecondaryContainer   = Color(0xFFBDEACD)
val md_theme_OnSecondaryContainer = Color(0xFF426B54)

// ── Tertiary ──
val md_theme_Tertiary            = Color(0xFF005236)
val md_theme_OnTertiary          = Color(0xFFFFFFFF)
val md_theme_TertiaryContainer   = Color(0xFF116C4A)
val md_theme_OnTertiaryContainer = Color(0xFF98EABF)
val md_theme_TertiaryFixed       = Color(0xFFA1F4C8)
val md_theme_TertiaryFixedDim    = Color(0xFF86D7AD)

// ── Error (Coral) ──
val md_theme_Error            = Color(0xFFBA1A1A)
val md_theme_OnError          = Color(0xFFFFFFFF)
val md_theme_ErrorContainer   = Color(0xFFFFDAD6)
val md_theme_OnErrorContainer = Color(0xFF93000A)

// ── Outline ──
val md_theme_Outline        = Color(0xFF707973)
val md_theme_OutlineVariant = Color(0xFFBFC9C1)

// ── Input Field Background (recessed style per DESIGN.md) ──
val md_theme_InputBg = Color(0xFFE9ECEF)

// ══════════════════════════════════════════════════════════════
// Backward-compatible aliases (old token names → new values)
// These allow gradual migration without breaking references.
// ══════════════════════════════════════════════════════════════

val md_theme_DarkBg        = md_theme_Surface
val md_theme_CardBg        = md_theme_SurfaceContainerLowest  // white cards
val md_theme_AccentGreen   = md_theme_Primary
val md_theme_TextPrimary   = md_theme_OnSurface
val md_theme_TextSecondary = md_theme_OnSurfaceVariant

// Status Colors (highly vibrant and context-appropriate)
val md_theme_StatusGreen  = Color(0xFF2E7D32)         
val md_theme_StatusOrange = Color(0xFFEF6C00)         
val md_theme_StatusRed    = Color(0xFFD32F2F)         

// Graph Colors (highly distinguishable, vibrant colors for X, Y, Z axes)
val md_theme_GraphX = Color(0xFFE53935)  
val md_theme_GraphY = Color(0xFF00897B)  
val md_theme_GraphZ = Color(0xFF1E88E5)  

// ── Google Font configuration for Sora ──
private val fontProvider = androidx.compose.ui.text.googlefonts.GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = com.pemalang.roaddamage.R.array.com_google_android_gms_fonts_certs
)

private val soraFont = androidx.compose.ui.text.googlefonts.GoogleFont("Sora")

val SoraFontFamily = androidx.compose.ui.text.font.FontFamily(
    androidx.compose.ui.text.googlefonts.Font(googleFont = soraFont, fontProvider = fontProvider, weight = androidx.compose.ui.text.font.FontWeight.Normal),
    androidx.compose.ui.text.googlefonts.Font(googleFont = soraFont, fontProvider = fontProvider, weight = androidx.compose.ui.text.font.FontWeight.Medium),
    androidx.compose.ui.text.googlefonts.Font(googleFont = soraFont, fontProvider = fontProvider, weight = androidx.compose.ui.text.font.FontWeight.SemiBold),
    androidx.compose.ui.text.googlefonts.Font(googleFont = soraFont, fontProvider = fontProvider, weight = androidx.compose.ui.text.font.FontWeight.Bold)
)

// ── Typography definition per DESIGN.md ──
val RoadDamageTypography = androidx.compose.material3.Typography(
    displayLarge = androidx.compose.ui.text.TextStyle(fontFamily = SoraFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
    displayMedium = androidx.compose.ui.text.TextStyle(fontFamily = SoraFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
    displaySmall = androidx.compose.ui.text.TextStyle(fontFamily = SoraFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
    headlineLarge = androidx.compose.ui.text.TextStyle(fontFamily = SoraFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
    headlineMedium = androidx.compose.ui.text.TextStyle(fontFamily = SoraFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
    headlineSmall = androidx.compose.ui.text.TextStyle(fontFamily = SoraFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
    titleLarge = androidx.compose.ui.text.TextStyle(fontFamily = SoraFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
    titleMedium = androidx.compose.ui.text.TextStyle(fontFamily = SoraFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
    titleSmall = androidx.compose.ui.text.TextStyle(fontFamily = SoraFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
    bodyLarge = androidx.compose.ui.text.TextStyle(fontFamily = SoraFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal),
    bodyMedium = androidx.compose.ui.text.TextStyle(fontFamily = SoraFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal),
    bodySmall = androidx.compose.ui.text.TextStyle(fontFamily = SoraFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal),
    labelLarge = androidx.compose.ui.text.TextStyle(fontFamily = SoraFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
    labelMedium = androidx.compose.ui.text.TextStyle(fontFamily = SoraFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
    labelSmall = androidx.compose.ui.text.TextStyle(fontFamily = SoraFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
)

private val LightColorScheme = androidx.compose.material3.lightColorScheme(
    primary = md_theme_Primary,
    onPrimary = md_theme_OnPrimary,
    primaryContainer = md_theme_PrimaryContainer,
    onPrimaryContainer = md_theme_OnPrimaryContainer,
    secondary = md_theme_Secondary,
    onSecondary = md_theme_OnSecondary,
    secondaryContainer = md_theme_SecondaryContainer,
    onSecondaryContainer = md_theme_OnSecondaryContainer,
    tertiary = md_theme_Tertiary,
    onTertiary = md_theme_OnTertiary,
    tertiaryContainer = md_theme_TertiaryContainer,
    onTertiaryContainer = md_theme_OnTertiaryContainer,
    error = md_theme_Error,
    onError = md_theme_OnError,
    errorContainer = md_theme_ErrorContainer,
    onErrorContainer = md_theme_OnErrorContainer,
    background = md_theme_Surface,
    onBackground = md_theme_OnSurface,
    surface = md_theme_Surface,
    onSurface = md_theme_OnSurface,
    surfaceVariant = md_theme_SurfaceVariant,
    onSurfaceVariant = md_theme_OnSurfaceVariant,
    outline = md_theme_Outline,
    outlineVariant = md_theme_OutlineVariant
)

@Composable
fun RoadDamageTheme(content: @Composable () -> Unit) {
    androidx.compose.material3.MaterialTheme(
        colorScheme = LightColorScheme,
        typography = RoadDamageTypography,
        content = content
    )
}
