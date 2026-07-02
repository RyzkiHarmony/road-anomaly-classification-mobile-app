package com.pemalang.roaddamage.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable


// ── Surface & Background ──
val md_theme_Surface         = Color(0xFFF8FAFC) // Slate 50
val md_theme_SurfaceDim      = Color(0xFFF1F5F9) // Slate 100
val md_theme_SurfaceBright   = Color(0xFFFFFFFF)
val md_theme_SurfaceContainerLowest = Color(0xFFFFFFFF)
val md_theme_SurfaceContainerLow    = Color(0xFFF8FAFC)
val md_theme_SurfaceContainer       = Color(0xFFF1F5F9)
val md_theme_SurfaceContainerHigh   = Color(0xFFE2E8F0)
val md_theme_SurfaceContainerHighest = Color(0xFFCBD5E1)
val md_theme_SurfaceVariant  = Color(0xFFE2E8F0)

// ── On Surface (Text) ──
val md_theme_OnSurface        = Color(0xFF0F172A) // Slate 900
val md_theme_OnSurfaceVariant = Color(0xFF475569) // Slate 600
val md_theme_InverseSurface   = Color(0xFF0F172A)
val md_theme_InverseOnSurface = Color(0xFFF8FAFC)

// ── Primary (Pastel Green) ──
val md_theme_Primary            = Color(0xFF42D392)
val md_theme_OnPrimary          = Color(0xFFFFFFFF)
val md_theme_PrimaryContainer   = Color(0xFFD4F5E6) 
val md_theme_OnPrimaryContainer = Color(0xFF0D3D27) 
val md_theme_InversePrimary     = Color(0xFF7DE6B9) 
val md_theme_PrimaryFixed       = Color(0xFFD4F5E6)
val md_theme_PrimaryFixedDim    = Color(0xFFA8EFCE) 
val md_theme_SurfaceTint        = Color(0xFF42D392)

// ── Secondary (Slate 700) ──
val md_theme_Secondary            = Color(0xFF334155)
val md_theme_OnSecondary          = Color(0xFFFFFFFF)
val md_theme_SecondaryContainer   = Color(0xFFE2E8F0) // Slate 200
val md_theme_OnSecondaryContainer = Color(0xFF0F172A) // Slate 900

// ── Tertiary (Cyan 600) ──
val md_theme_Tertiary            = Color(0xFF0891B2)
val md_theme_OnTertiary          = Color(0xFFFFFFFF)
val md_theme_TertiaryContainer   = Color(0xFFCFFAFE) // Cyan 100
val md_theme_OnTertiaryContainer = Color(0xFF164E63) // Cyan 900

// ── Error (Red 500) ──
val md_theme_Error            = Color(0xFFEF4444)
val md_theme_OnError          = Color(0xFFFFFFFF)
val md_theme_ErrorContainer   = Color(0xFFFEE2E2) // Red 100
val md_theme_OnErrorContainer = Color(0xFF7F1D1D) // Red 900

// ── Outline ──
val md_theme_Outline        = Color(0xFF94A3B8) // Slate 400
val md_theme_OutlineVariant = Color(0xFFCBD5E1) // Slate 300

// ── Input Field Background ──
val md_theme_InputBg = Color(0xFFF1F5F9)

val md_theme_DarkBg        = md_theme_Surface
val md_theme_CardBg        = md_theme_SurfaceContainerLowest  // white cards
val md_theme_AccentGreen   = md_theme_Primary
val md_theme_TextPrimary   = md_theme_OnSurface
val md_theme_TextSecondary = md_theme_OnSurfaceVariant

// Status Colors (Vibrant Dashboard Standard)
val md_theme_StatusGreen  = Color(0xFF10B981)         
val md_theme_StatusOrange = Color(0xFFF59E0B)         
val md_theme_StatusRed    = Color(0xFFEF4444)         

// Graph Colors (Professional distinguishable)
val md_theme_GraphX = Color(0xFFEF4444)  // Red
val md_theme_GraphY = Color(0xFF10B981)  // Green
val md_theme_GraphZ = Color(0xFF3B82F6)  // Blue

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
