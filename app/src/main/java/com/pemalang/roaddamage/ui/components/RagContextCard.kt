package com.pemalang.roaddamage.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pemalang.roaddamage.data.remote.AnomalyContextResponse

@Composable
fun RagContextCard(
    isLoading: Boolean,
    context: AnomalyContextResponse?,
    modifier: Modifier = Modifier,
    onActionClick: (String) -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: AI Badge + Severity Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Context",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Wawasan AI",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }

                if (context != null && !isLoading) {
                    val (severityBg, severityText, label) = when (context.severity.uppercase()) {
                        "HIGH" -> Triple(
                            Color(0xFFEF5350).copy(alpha = 0.2f),
                            Color(0xFFD32F2F),
                            "RISIKO TINGGI"
                        )
                        "LOW" -> Triple(
                            Color(0xFF66BB6A).copy(alpha = 0.2f),
                            Color(0xFF2E7D32),
                            "RISIKO RENDAH"
                        )
                        else -> Triple(
                            Color(0xFFFFA726).copy(alpha = 0.2f),
                            Color(0xFFE65100),
                            "RISIKO SEDANG"
                        )
                    }

                    Surface(
                        color = severityBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = severityText
                        )
                    }
                }
            }

            // Body: Loading or Content
            if (isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Menganalisis riwayat & konteks via Gemini AI...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (context != null) {
                // Gemini AI Summary
                Text(
                    text = context.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )

                // Stats Grid
                val stats = context.stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBadge(
                        label = "Kasus Mirip",
                        value = "${stats.similarCases}x",
                        modifier = Modifier.weight(1f)
                    )
                    StatBadge(
                        label = "Status Titik",
                        value = if (stats.locationHotspot) "Hotspot" else "Normal",
                        modifier = Modifier.weight(1f)
                    )
                    StatBadge(
                        label = "Tren Jalan",
                        value = when (stats.trend.lowercase()) {
                            "worsening" -> "Memburuk"
                            "improving" -> "Membaik"
                            else -> "Stabil"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Confidence assessment explanation
                if (context.confidenceAssessment.isNotEmpty()) {
                    Text(
                        text = "Evaluasi: ${context.confidenceAssessment}",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                // Recommendations Chips
                if (context.recommendations.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        context.recommendations.take(2).forEach { rec ->
                            FilledTonalButton(
                                onClick = { onActionClick(rec.action) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (rec.icon == "report") Icons.Default.Info else Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = rec.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Ketuk untuk melihat wawasan cerdas anomali ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
