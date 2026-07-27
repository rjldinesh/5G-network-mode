package net.peaksoftstudios.fiveg.networkmode.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.peaksoftstudios.fiveg.networkmode.manager.SignalEntry

@Composable
fun SignalHistoryChart(entries: List<SignalEntry>, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceVariant

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Signal History",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Last ${entries.size} readings",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(Modifier.height(8.dp))

            if (entries.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Collecting data…",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    drawChart(entries, primary)
                }

                Spacer(Modifier.height(6.dp))

                // Y-axis labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val min = entries.minOf { it.dbm }
                    val max = entries.maxOf { it.dbm }
                    Text("$min dBm", fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("$max dBm", fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
    }
}

private fun DrawScope.drawChart(entries: List<SignalEntry>, lineColor: Color) {
    if (entries.size < 2) return

    val minDbm = entries.minOf { it.dbm }.toFloat()
    val maxDbm = entries.maxOf { it.dbm }.toFloat()
    val range = (maxDbm - minDbm).coerceAtLeast(1f)

    val w = size.width
    val h = size.height
    val padV = 8.dp.toPx()

    fun xAt(i: Int) = i * (w / (entries.size - 1))
    fun yAt(dbm: Int) = h - padV - ((dbm - minDbm) / range) * (h - 2 * padV)

    // Filled gradient under the line
    val fillPath = Path().apply {
        moveTo(xAt(0), h)
        entries.forEachIndexed { i, e -> lineTo(xAt(i), yAt(e.dbm)) }
        lineTo(xAt(entries.lastIndex), h)
        close()
    }
    drawPath(
        fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent),
            startY = 0f, endY = h
        )
    )

    // Line
    val linePath = Path().apply {
        entries.forEachIndexed { i, e ->
            val x = xAt(i); val y = yAt(e.dbm)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
    }
    drawPath(linePath, color = lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

    // Dots at each point
    entries.forEachIndexed { i, e ->
        drawCircle(lineColor, radius = 3.dp.toPx(), center = Offset(xAt(i), yAt(e.dbm)))
    }
}
