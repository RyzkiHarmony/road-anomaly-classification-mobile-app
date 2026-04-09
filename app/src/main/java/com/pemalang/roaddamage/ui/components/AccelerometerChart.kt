package com.pemalang.roaddamage.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pemalang.roaddamage.ui.theme.md_theme_TextSecondary
import com.pemalang.roaddamage.ui.theme.md_theme_GraphZ
import com.pemalang.roaddamage.ui.theme.md_theme_GraphX
import com.pemalang.roaddamage.ui.theme.md_theme_GraphY

// ── Design tokens ──
private val TextSecondary = md_theme_TextSecondary
private val GraphLineZ = md_theme_GraphZ
private val GraphLineX = md_theme_GraphX
private val GraphLineY = md_theme_GraphY

/**
 * Three-axis accelerometer line chart (X / Y / Z).
 *
 * Each axis is rendered as a coloured polyline inside a [Canvas].
 * The chart auto-scales to the min/max across all three axes.
 */
@Composable
fun Chart3Lines(ax: FloatArray, ay: FloatArray, az: FloatArray, modifier: Modifier) {
    val minV = minOf(ax.minOrNull() ?: -12f, ay.minOrNull() ?: -12f, az.minOrNull() ?: -12f)
    val maxV = maxOf(ax.maxOrNull() ?: 12f, ay.maxOrNull() ?: 12f, az.maxOrNull() ?: 12f)
    val range = (maxV - minV).let { if (it < 1e-3f) 1f else it }

    Canvas(modifier = modifier) {
        // Draw centre grid line
        val midY = size.height / 2
        drawLine(Color(0xFF2C3240), Offset(0f, midY), Offset(size.width, midY))

        fun drawSeries(values: FloatArray, color: Color) {
            if (values.isEmpty()) return
            val n = values.size
            val stepX = if (n > 1) size.width / (n - 1) else size.width
            val path = Path()
            for (i in 0 until n) {
                val x = i * stepX
                val v = values[i]
                val y = size.height - ((v - minV) / range) * size.height
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx()))
        }

        drawSeries(az, GraphLineZ)
        drawSeries(ax, GraphLineX)
        drawSeries(ay, GraphLineY)
    }
}

/**
 * Small coloured dot + label used as a chart legend entry.
 */
@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}
