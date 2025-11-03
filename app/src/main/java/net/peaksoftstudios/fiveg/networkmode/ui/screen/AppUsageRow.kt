package net.peaksoftstudios.fiveg.networkmode.ui.screen


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import net.peaksoftstudios.fiveg.networkmode.ui.screen.viewmodel.AppDataUsage

@Composable
fun AppUsageRow(app: AppDataUsage) {
    val context = LocalContext.current
    val pm = context.packageManager

    val iconBitmap = remember(app.packageName) {
        try {
            val drawable = pm.getApplicationIcon(app.packageName)
            drawable.toBitmap(96, 96).asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = 12.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = "Default App Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = 12.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text("Download", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                        Text(formatBytes(app.downloadBytes), style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Upload", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                        Text(formatBytes(app.uploadBytes), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
fun formatBytes(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024

    return when {
        bytes >= gb -> String.format("%.2f GB", bytes / gb)
        bytes >= mb -> String.format("%.2f MB", bytes / mb)
        bytes >= kb -> String.format("%.2f KB", bytes / kb)
        else -> "$bytes B"
    }
}