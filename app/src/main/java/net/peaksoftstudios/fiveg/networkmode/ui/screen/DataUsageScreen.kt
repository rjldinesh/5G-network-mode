package net.peaksoftstudios.fiveg.networkmode.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import net.peaksoftstudios.fiveg.networkmode.manager.DataLimitManager
import net.peaksoftstudios.fiveg.networkmode.ui.components.HeroCaption
import net.peaksoftstudios.fiveg.networkmode.ui.components.HeroCard
import net.peaksoftstudios.fiveg.networkmode.ui.components.HeroEyebrow
import net.peaksoftstudios.fiveg.networkmode.ui.components.HeroPill
import net.peaksoftstudios.fiveg.networkmode.ui.components.PermissionCard
import net.peaksoftstudios.fiveg.networkmode.ui.components.PrimaryActionButton
import net.peaksoftstudios.fiveg.networkmode.ui.components.SectionEyebrow
import net.peaksoftstudios.fiveg.networkmode.ui.screen.viewmodel.DataUsageUiState
import net.peaksoftstudios.fiveg.networkmode.ui.screen.viewmodel.DataUsageViewModel
import net.peaksoftstudios.fiveg.networkmode.ui.theme.Amber
import net.peaksoftstudios.fiveg.networkmode.ui.theme.Danger
import net.peaksoftstudios.fiveg.networkmode.ui.theme.LavenderOnNavy
import net.peaksoftstudios.fiveg.networkmode.ui.theme.Mint
import net.peaksoftstudios.fiveg.networkmode.utils.PermissionUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DataUsageScreen(viewModel: DataUsageViewModel) {
    val state by viewModel.uiState
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showLimitDialog by remember { mutableStateOf(false) }
    var limitInput by remember { mutableStateOf("") }
    var currentLimitMb by remember { mutableLongStateOf(DataLimitManager.getLimit(context)) }

    // Reload when coming back from the Usage Access settings screen.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && PermissionUtils.hasUsagePermission(context)) {
                viewModel.loadData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showLimitDialog) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Monthly data limit", style = MaterialTheme.typography.titleSmall) },
            text = {
                Column {
                    Text(
                        "Enter your monthly mobile data limit in MB.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = limitInput,
                        onValueChange = { limitInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Limit (MB)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mb = limitInput.toLongOrNull() ?: 0L
                        if (mb > 0) {
                            DataLimitManager.setLimit(context, mb)
                            currentLimitMb = mb
                        }
                        showLimitDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Save", style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = {
                TextButton(onClick = { showLimitDialog = false }) {
                    Text("Cancel", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    val apps = (state as? DataUsageUiState.Success)?.data.orEmpty()
    val totalBytes = apps.sumOf { it.downloadBytes + it.uploadBytes }
    val maxBytes = apps.maxOfOrNull { it.downloadBytes + it.uploadBytes } ?: 0L

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            UsageHero(
                totalBytes = totalBytes,
                limitMb = currentLimitMb,
                onSetLimit = { showLimitDialog = true }
            )
        }

        when (val s = state) {
            is DataUsageUiState.Loading -> item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Mint, trackColor = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(12.dp))
                    Text("Loading usage…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            is DataUsageUiState.Success -> {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionEyebrow("By app")
                        Text(
                            "Most used",
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.sp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                items(s.data, key = { it.packageName }) { app ->
                    AppUsageRow(app = app, maxBytes = maxBytes)
                }
            }

            is DataUsageUiState.Empty -> item {
                Text(
                    "No network data recorded this month yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                )
            }

            is DataUsageUiState.Error -> item {
                if (s.message.contains("Usage Access")) {
                    PermissionCard(
                        title = "Usage access required",
                        message = "Allow usage access so the app can read per-app mobile and Wi‑Fi data totals.",
                        icon = Icons.Filled.DataUsage
                    ) {
                        PrimaryActionButton(text = "Grant usage access", height = 48.dp) {
                            PermissionUtils.openUsageAccessSettings(context)
                        }
                    }
                } else {
                    Text(
                        "Error: ${s.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UsageHero(totalBytes: Long, limitMb: Long, onSetLimit: () -> Unit) {
    val limitBytes = limitMb * 1_000_000L
    val progress = if (limitMb > 0) (totalBytes.toFloat() / limitBytes).coerceIn(0f, 1f) else 0f
    val barColor = when {
        limitMb <= 0 -> Mint
        progress >= 1f -> Danger
        progress >= 0.8f -> Amber
        else -> Mint
    }
    val (value, unit) = splitBytes(totalBytes)
    val unitLabel = if (limitMb > 0) "$unit of ${formatLimit(limitMb)}" else "$unit used"

    val cal = Calendar.getInstance()
    val monthFmt = SimpleDateFormat("MMM", Locale.getDefault())
    val today = cal.get(Calendar.DAY_OF_MONTH)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val rangeLabel = "${monthFmt.format(cal.time)} 1 – $today"
    val resetsIn = daysInMonth - today
    val resetLabel = when (resetsIn) {
        0 -> "Resets tomorrow"
        1 -> "Resets in 1 day"
        else -> "Resets in $resetsIn days"
    }

    HeroCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeroEyebrow("This month")
            HeroPill(text = "Limit", icon = Icons.Filled.Edit, onClick = onSetLimit)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(value, style = MaterialTheme.typography.displayMedium, color = Color.White)
            Text(
                unitLabel,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = LavenderOnNavy,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.16f))
        ) {
            if (limitMb > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .widthIn(min = 10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(barColor)
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            HeroCaption(rangeLabel)
            HeroCaption(
                when {
                    limitMb > 0 -> "${(progress * 100).toInt()}% · $resetLabel"
                    else -> "No limit set · $resetLabel"
                }
            )
        }
    }
}

/** "48.6" to "MB" — decimal units, matching DataLimitManager. */
private fun splitBytes(bytes: Long): Pair<String, String> = when {
    bytes >= 1_000_000_000L -> "%.2f".format(bytes / 1_000_000_000.0) to "GB"
    bytes >= 1_000_000L -> "%.1f".format(bytes / 1_000_000.0) to "MB"
    bytes >= 1_000L -> "%.0f".format(bytes / 1_000.0) to "KB"
    else -> bytes.toString() to "B"
}

private fun formatLimit(limitMb: Long): String =
    if (limitMb >= 1000) "%.1f GB".format(limitMb / 1000.0).replace(".0 GB", " GB") else "$limitMb MB"
