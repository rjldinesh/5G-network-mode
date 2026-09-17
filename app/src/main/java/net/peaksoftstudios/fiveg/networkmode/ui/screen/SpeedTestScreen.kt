package net.peaksoftstudios.fiveg.networkmode.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.peaksoftstudios.fiveg.networkmode.ui.components.AppCard
import net.peaksoftstudios.fiveg.networkmode.ui.components.PrimaryActionButton
import net.peaksoftstudios.fiveg.networkmode.ui.components.SecondaryActionButton
import net.peaksoftstudios.fiveg.networkmode.ui.screen.viewmodel.SpeedTestState
import net.peaksoftstudios.fiveg.networkmode.ui.screen.viewmodel.SpeedTestViewModel
import net.peaksoftstudios.fiveg.networkmode.ui.theme.Amber
import net.peaksoftstudios.fiveg.networkmode.ui.theme.Danger
import net.peaksoftstudios.fiveg.networkmode.ui.theme.Mint
import net.peaksoftstudios.fiveg.networkmode.ui.theme.MintDark
import kotlin.math.min

/** Speed test card: a ring with the current state inside and the action button below. */
@Composable
fun SpeedTestScreen(modifier: Modifier = Modifier) {
    val viewModel: SpeedTestViewModel = viewModel()
    val state by viewModel.state

    AppCard(modifier = modifier, radius = 20.dp, contentPadding = PaddingValues(20.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val s = state) {
                is SpeedTestState.Idle -> {
                    SpeedRing(progress = 80f / 360f) {
                        Text("Ready", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                        Text("Ping · Down · Up", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    PrimaryActionButton(
                        text = "Start speed test",
                        icon = Icons.Filled.Speed,
                        height = 50.dp,
                        onClick = { viewModel.startTest() }
                    )
                }

                is SpeedTestState.Running -> {
                    val animated by animateFloatAsState(targetValue = s.progress, animationSpec = tween(600), label = "speedProgress")
                    SpeedRing(progress = animated) {
                        Text("${(animated * 100).toInt()}%", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                        Text(s.phase.removeSuffix("..."), style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                    PrimaryActionButton(text = "Testing…", height = 50.dp, enabled = false, onClick = {})
                }

                is SpeedTestState.Success -> {
                    val quality = networkQuality(s.result.pingMs, s.result.downloadMbps)
                    SpeedRing(progress = 1f, ringColor = quality.second) {
                        Text("%.1f".format(s.result.downloadMbps), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                        Text("Mbps down", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SpeedStat(Modifier.weight(1f), "Ping", "${s.result.pingMs}", "ms")
                        SpeedStat(Modifier.weight(1f), "Download", "%.1f".format(s.result.downloadMbps), "Mbps")
                        SpeedStat(Modifier.weight(1f), "Upload", "%.1f".format(s.result.uploadMbps), "Mbps")
                    }
                    Surface(
                        color = quality.second.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(quality.second)
                            )
                            Column {
                                Text(quality.first, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold), color = quality.second)
                                Text(quality.third, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    SecondaryActionButton(text = "Run again", icon = Icons.Filled.Refresh, onClick = { viewModel.reset() })
                }

                is SpeedTestState.Error -> {
                    SpeedRing(progress = 1f, ringColor = Danger) {
                        Text("Failed", style = MaterialTheme.typography.headlineSmall, color = Danger)
                        Text("Check your connection", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        s.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    PrimaryActionButton(text = "Retry", icon = Icons.Filled.Refresh, height = 50.dp, onClick = { viewModel.startTest() })
                }
            }
        }
    }
}

@Composable
private fun SpeedRing(
    progress: Float,
    ringColor: Color = Mint,
    content: @Composable () -> Unit
) {
    val track = MaterialTheme.colorScheme.outline
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(156.dp)) {
        Canvas(modifier = Modifier.size(156.dp)) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(20.dp)
        ) { content() }
    }
}

@Composable
private fun SpeedStat(modifier: Modifier, label: String, value: String, unit: String) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
            Spacer(Modifier.width(2.dp))
            Text(unit, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

private fun networkQuality(pingMs: Long, downloadMbps: Double): Triple<String, Color, String> {
    val score = min(100.0, (downloadMbps / 100.0 * 60) + ((1000.0 - pingMs.coerceAtMost(1000)) / 1000.0 * 40))
    return when {
        score >= 80 -> Triple("Excellent", MintDark, "Great for streaming & gaming")
        score >= 55 -> Triple("Good", MintDark, "Good for most tasks")
        score >= 30 -> Triple("Fair", Amber, "Suitable for browsing")
        else -> Triple("Poor", Danger, "Limited connectivity")
    }
}
