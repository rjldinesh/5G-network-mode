package net.peaksoftstudios.fiveg.networkmode.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.peaksoftstudios.fiveg.networkmode.ui.theme.Amber
import net.peaksoftstudios.fiveg.networkmode.ui.theme.Lime
import net.peaksoftstudios.fiveg.networkmode.ui.theme.Mint
import net.peaksoftstudios.fiveg.networkmode.ui.theme.MintDark

private const val MIN_DBM = -120f
private const val MAX_DBM = -50f

/** Colour used for the quality label (and stat tiles) for a given signal level 0–4. */
fun signalQualityColor(level: Int): Color = when (level.coerceIn(0, 4)) {
    4, 3 -> MintDark
    2 -> Lime
    1 -> Amber
    else -> Color(0xFFE5484D)
}

/**
 * Half-circle signal gauge: amber → lime → mint sweep on a grey track,
 * with the dBm reading and quality label sitting inside the arc.
 */
@Composable
fun AnimatedSignalStrengthGauge(
    signalLevel: Int,     // 0–4
    signalDbm: Int?,      // e.g. -85
    modifier: Modifier = Modifier
) {
    val clampedLevel = signalLevel.coerceIn(0, 4)
    // Fill by quality band (matches the design: Poor 25%, Fair 50%, Good 75%, Excellent ~95%),
    // nudged within the band by the raw dBm so small changes are still visible.
    val bandStart = when (clampedLevel) { 4 -> 0.80f; 3 -> 0.60f; 2 -> 0.40f; 1 -> 0.18f; else -> 0.0f }
    val bandEnd = when (clampedLevel) { 4 -> 0.97f; 3 -> 0.80f; 2 -> 0.60f; 1 -> 0.40f; else -> 0.06f }
    val within = if (signalDbm != null && signalDbm != 0) ((signalDbm - MIN_DBM) / (MAX_DBM - MIN_DBM)).coerceIn(0f, 1f) else 0.5f
    val target = bandStart + (bandEnd - bandStart) * within
    val progress by animateFloatAsState(targetValue = target, animationSpec = tween(700), label = "signalGauge")
    val track = MaterialTheme.colorScheme.outline

    val label = when (clampedLevel) {
        4 -> "Excellent"
        3 -> "Good"
        2 -> "Fair"
        1 -> "Poor"
        else -> "No signal"
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(104.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 18.dp.toPx()
                val diameter = size.width - stroke
                val topLeft = Offset(stroke / 2, stroke / 2)
                val arcSize = Size(diameter, diameter)

                drawArc(
                    color = track,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Butt)
                )

                val brush = Brush.sweepGradient(
                    colorStops = arrayOf(
                        0.5f to Amber,
                        0.75f to Lime,
                        0.88f to Mint,
                        1f to Mint
                    ),
                    center = Offset(size.width / 2, stroke / 2 + diameter / 2)
                )
                drawArc(
                    brush = brush,
                    startAngle = 180f,
                    sweepAngle = 180f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Butt)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (signalDbm != null && signalDbm != 0) "$signalDbm dBm" else "—",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.sp),
                    color = signalQualityColor(clampedLevel)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.width(200.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val faint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            Text("-120", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = faint)
            Text("dBm", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = faint)
            Text("-50", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = faint)
        }
    }
}
