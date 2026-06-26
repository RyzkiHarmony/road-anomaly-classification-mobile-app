package com.pemalang.roaddamage.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import com.pemalang.roaddamage.ui.theme.md_theme_Surface
import com.pemalang.roaddamage.ui.theme.md_theme_Primary
import com.pemalang.roaddamage.ui.theme.md_theme_OnSurface
import com.pemalang.roaddamage.ui.theme.md_theme_OnSurfaceVariant

@Composable
fun SplashScreen(
    onFinished: (isOnboardingCompleted: Boolean) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
        delay(800)
        val completed = viewModel.isOnboardingCompleted()
        onFinished(completed)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(md_theme_Surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alpha.value)
        ) {
            Image(
                imageVector = Icons.Default.Terrain,
                contentDescription = "Logo",
                colorFilter = ColorFilter.tint(md_theme_Primary),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Road Damage Detector",
                color = md_theme_OnSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Crowdsourcing Road Quality",
                color = md_theme_OnSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}
