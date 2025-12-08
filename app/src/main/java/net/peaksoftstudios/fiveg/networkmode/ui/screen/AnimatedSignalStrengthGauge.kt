package net.peaksoftstudios.fiveg.networkmode.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedSignalStrengthGauge(
    signalLevel: Int,     // 0–4
    signalDbm: Int?,      // e.g. -85
    modifier: Modifier = Modifier
) {
    val clampedLevel = signalLevel.coerceIn(0, 4)
    val targetProgress = clampedLevel / 4f
    val animatedProgress by animateFloatAsState(targetValue = targetProgress, label = "signalAnimation")

    // Gradient colors
    val colorStops = listOf(
        0f to Color.Red,
        0.25f to Color(0xFFFFA500),
        0.5f to Color.Yellow,
        0.75f to Color(0xFF4CAF50),
        1f to Color(0xFF00C853)
    )
    val brush = Brush.sweepGradient(colorStops.map { it.second })

    val label = when (clampedLevel) {
        4 -> "Excellent"
        3 -> "Good"
        2 -> "Fair"
        1 -> "Poor"
        else -> "No Signal"
    }

    // --- UI ---
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.4f)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension / 14
            val radius = size.minDimension / 2 - strokeWidth
            val arcSize = Size(radius * 2, radius * 2)
            val topLeft = Offset(center.x - radius, center.y - radius)

            val startAngle = 135f      // Start from bottom-left
            val totalSweep = 270f      // Cover 3/4 circle (270°)
            val sweep = totalSweep * animatedProgress

            // Background arc (light gray)
            drawArc(
                color = Color.LightGray.copy(alpha = 0.3f),
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )

            // Foreground progress arc
            drawArc(
                brush = brush,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Text overlay in center
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black)

            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (signalDbm != null) "$signalDbm dBm" else "—",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Black)
            )
        }
    }
}
