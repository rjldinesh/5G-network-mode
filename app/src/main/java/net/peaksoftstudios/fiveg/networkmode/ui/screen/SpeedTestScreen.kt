package net.peaksoftstudios.fiveg.networkmode.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.peaksoftstudios.fiveg.networkmode.ui.screen.viewmodel.SpeedTestState
import net.peaksoftstudios.fiveg.networkmode.ui.screen.viewmodel.SpeedTestViewModel
import kotlin.math.min

@Composable
fun SpeedTestScreen() {
val viewModel: SpeedTestViewModel = viewModel()
    val state by viewModel.state

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        when (val s = state) {
            is SpeedTestState.Idle -> {
                IdleView(onStart = { viewModel.startTest() })
            }

            is SpeedTestState.Running -> {
                RunningView(phase = s.phase, progress = s.progress)
            }

            is SpeedTestState.Success -> {
                ResultView(
                    pingMs = s.result.pingMs,
                    downloadMbps = s.result.downloadMbps,
                    uploadMbps = s.result.uploadMbps,
                    onRetest = { viewModel.reset() }
                )
            }

            is SpeedTestState.Error -> {
                ErrorView(message = s.message, onRetry = { viewModel.startTest() })
            }
        }
    }
}

@Composable
private fun IdleView(onStart: () -> Unit) {
    Spacer(Modifier.height(40.dp))

    // Decorative ring
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(200.dp)
    ) {
        SpeedRingDecoration()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Ready", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Text("Tap to test", fontSize = 13.sp, color = Color.Gray)
        }
    }

    Spacer(Modifier.height(32.dp))

    Button(
        onClick = onStart,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text("Start Speed Test", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }

    Spacer(Modifier.height(24.dp))

    Text(
        "Tests your real-time network\nping, download & upload speed.",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodySmall,
        color = Color.Gray
    )
}

@Composable
private fun RunningView(phase: String, progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(600),
        label = "progress"
    )

    Spacer(Modifier.height(24.dp))

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2
            drawArc(
                color = Color.LightGray.copy(alpha = 0.3f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(Color(0xFF00B3FF), Color(0xFF00FFBF))
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${(animatedProgress * 100).toInt()}%",
                fontSize = 28.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    Text(phase, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)

    Spacer(Modifier.height(8.dp))

    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    )
}

@Composable
private fun ResultView(
    pingMs: Long,
    downloadMbps: Double,
    uploadMbps: Double,
    onRetest: () -> Unit
) {
    Text("Speed Test Results", fontSize = 20.sp, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground)

    Spacer(Modifier.height(24.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SpeedCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.NetworkPing,
            label = "Ping",
            value = "${pingMs}",
            unit = "ms",
            color = Color(0xFFFF9800)
        )
        SpeedCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.CloudDownload,
            label = "Download",
            value = "%.1f".format(downloadMbps),
            unit = "Mbps",
            color = Color(0xFF00B3FF)
        )
    }

    Spacer(Modifier.height(12.dp))

    SpeedCard(
        modifier = Modifier.fillMaxWidth(),
        icon = Icons.Filled.CloudUpload,
        label = "Upload",
        value = "%.1f".format(uploadMbps),
        unit = "Mbps",
        color = Color(0xFF00FFBF)
    )

    Spacer(Modifier.height(24.dp))

    // Quality badge
    val quality = networkQuality(pingMs, downloadMbps)
    Surface(
        color = quality.second.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(quality.first, fontSize = 32.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Network Quality", fontWeight = FontWeight.SemiBold,
                    color = quality.second, fontSize = 15.sp)
                Text(quality.third, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }

    Spacer(Modifier.height(24.dp))

    OutlinedButton(
        onClick = onRetest,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text("Run Again", fontSize = 15.sp)
    }
}

@Composable
private fun SpeedCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.08f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = color)
                Spacer(Modifier.width(3.dp))
                Text(unit, fontSize = 12.sp, color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Spacer(Modifier.height(40.dp))
    Text("Test Failed", fontSize = 20.sp, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.error)
    Spacer(Modifier.height(8.dp))
    Text(message, style = MaterialTheme.typography.bodySmall, color = Color.Gray,
        textAlign = TextAlign.Center)
    Spacer(Modifier.height(24.dp))
    Button(onClick = onRetry) { Text("Retry") }
}

@Composable
private fun SpeedRingDecoration() {
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.size(200.dp)) {
        val stroke = 10.dp.toPx()
        val inset = stroke / 2
        drawArc(
            brush = Brush.sweepGradient(listOf(primary.copy(alpha = 0.2f), primary.copy(alpha = 0.6f))),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

private fun networkQuality(pingMs: Long, downloadMbps: Double): Triple<String, Color, String> {
    val score = min(100.0, (downloadMbps / 100.0 * 60) + ((1000.0 - pingMs.coerceAtMost(1000)) / 1000.0 * 40))
    return when {
        score >= 80 -> Triple("Excellent", Color(0xFF00C853), "Great for streaming & gaming")
        score >= 55 -> Triple("Good", Color(0xFF00B3FF), "Good for most tasks")
        score >= 30 -> Triple("Fair", Color(0xFFFF9800), "Suitable for browsing")
        else -> Triple("Poor", Color(0xFFFF3366), "Limited connectivity")
    }
}
