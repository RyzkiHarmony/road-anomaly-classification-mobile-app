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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

import com.pemalang.roaddamage.ui.theme.md_theme_Surface
import com.pemalang.roaddamage.ui.theme.md_theme_Primary
import com.pemalang.roaddamage.ui.theme.md_theme_OnPrimary
import com.pemalang.roaddamage.ui.theme.md_theme_OnSurface
import com.pemalang.roaddamage.ui.theme.md_theme_OnSurfaceVariant
import com.pemalang.roaddamage.ui.theme.md_theme_PrimaryFixed

@Composable
fun OnboardingScreen(
    onContinue: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    Box(modifier = Modifier.fillMaxSize().background(md_theme_Surface)) {
        Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Icon / Illustration — soft sage tinted circle
            Box(
                    modifier =
                            Modifier.size(160.dp)
                                    .background(
                                            md_theme_PrimaryFixed.copy(alpha = 0.3f),
                                            RoundedCornerShape(80.dp)
                                    ),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        imageVector = Icons.Default.AddRoad,
                        contentDescription = null,
                        tint = md_theme_Primary,
                        modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Title
            Text(
                    text = "Selamat Datang di\nRoad Damage Detector",
                    color = md_theme_OnSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                    text =
                            "Bantu kami memetakan kualitas jalan di sekitarmu. Cukup nyalakan aplikasi saat berkendara, dan sensor akan mendeteksi guncangan secara otomatis.",
                    color = md_theme_OnSurfaceVariant,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            // Button — Sage Green per DESIGN.md
            Button(
                    onClick = { viewModel.completeOnboarding(onContinue) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors =
                            ButtonDefaults.buttonColors(
                                    containerColor = md_theme_Primary,
                                    contentColor = md_theme_OnPrimary
                            ),
                    shape = RoundedCornerShape(12.dp)
            ) { Text(text = "Mulai Sekarang", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
