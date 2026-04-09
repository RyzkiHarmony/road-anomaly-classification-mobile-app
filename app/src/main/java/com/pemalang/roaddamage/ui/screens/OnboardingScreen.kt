package com.pemalang.roaddamage.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddRoad
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.pemalang.roaddamage.ui.theme.md_theme_DarkBg
import com.pemalang.roaddamage.ui.theme.md_theme_AccentGreen
import com.pemalang.roaddamage.ui.theme.md_theme_TextPrimary
import com.pemalang.roaddamage.ui.theme.md_theme_TextSecondary

private val DarkBg = md_theme_DarkBg
private val AccentGreen = md_theme_AccentGreen
private val TextPrimary = md_theme_TextPrimary
private val TextSecondary = md_theme_TextSecondary

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Icon / Illustration
            Box(
                    modifier =
                            Modifier.size(160.dp)
                                    .background(
                                            AccentGreen.copy(alpha = 0.1f),
                                            RoundedCornerShape(80.dp)
                                    ),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        imageVector = Icons.Default.AddRoad,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Title
            Text(
                    text = "Selamat Datang di\nRoad Damage Detector",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                    text =
                            "Bantu kami memetakan kualitas jalan di sekitarmu. Cukup nyalakan aplikasi saat berkendara, dan sensor akan mendeteksi guncangan secara otomatis.",
                    color = TextSecondary,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            // Button
            Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors =
                            ButtonDefaults.buttonColors(
                                    containerColor = AccentGreen,
                                    contentColor = DarkBg
                            ),
                    shape = RoundedCornerShape(12.dp)
            ) { Text(text = "Mulai Sekarang", fontSize = 16.sp, fontWeight = FontWeight.Bold) }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
