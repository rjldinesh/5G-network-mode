package net.peaksoftstudios.fiveg.networkmode.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.peaksoftstudios.fiveg.networkmode.manager.SignalEntry
import net.peaksoftstudios.fiveg.networkmode.ui.theme.Mint

private const val MAX_BARS = 12
private const val MIN_DBM = -120f
private const val MAX_DBM = -50f

/** Bar chart of the most recent signal readings, one mint bar per sample. */
@Composable
fun SignalHistoryChart(entries: List<SignalEntry>, modifier: Modifier = Modifier) {
    val recent = entries.takeLast(MAX_BARS)

    AppCard(modifier = modifier, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            SectionEyebrow("Signal history")
            Text(
                when (recent.size) { 0 -> "Waiting for readings"; 1 -> "Last reading"; else -> "Last ${recent.size} readings" },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        Spacer(Modifier.height(10.dp))

        if (recent.size < 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Collecting data…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                recent.forEach { entry ->
                    val fraction = ((entry.dbm - MIN_DBM) / (MAX_DBM - MIN_DBM)).coerceIn(0.08f, 1f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp * fraction)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Mint)
                    )
                }
            }
        }
    }
}
