package net.peaksoftstudios.fiveg.networkmode.ui.screen


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import net.peaksoftstudios.fiveg.networkmode.manager.DataLimitManager
import net.peaksoftstudios.fiveg.networkmode.ui.screen.viewmodel.DataUsageUiState
import net.peaksoftstudios.fiveg.networkmode.ui.screen.viewmodel.DataUsageViewModel
import net.peaksoftstudios.fiveg.networkmode.utils.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataUsageScreen(viewModel: DataUsageViewModel) {

    val state by viewModel.uiState
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showLimitDialog by remember { mutableStateOf(false) }
    var limitInput by remember { mutableStateOf("") }
    var currentLimitMb by remember { mutableStateOf(DataLimitManager.getLimit(context)) }

    // ✅ Automatically reload when coming back from Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (PermissionUtils.hasUsagePermission(context)) {
                    viewModel.loadData()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Set Limit Dialog
    if (showLimitDialog) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            title = { Text("Set Monthly Data Limit") },
            text = {
                Column {
                    Text("Enter your monthly mobile data limit in MB.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = limitInput,
                        onValueChange = { limitInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Limit (MB)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val mb = limitInput.toLongOrNull() ?: 0L
                    if (mb > 0) {
                        DataLimitManager.setLimit(context, mb)
                        currentLimitMb = mb
                    }
                    showLimitDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showLimitDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(topBar = { null }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ---- Data limit banner ----
            if (state is DataUsageUiState.Success) {
                val totalBytes = (state as DataUsageUiState.Success).data
                    .sumOf { it.downloadBytes + it.uploadBytes }
                DataLimitBanner(
                    totalBytes = totalBytes,
                    limitMb = currentLimitMb,
                    onSetLimit = { showLimitDialog = true }
                )
            }

            when (state) {
                is DataUsageUiState.Loading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading data...", color = Color.Gray)
                    }
                }

                is DataUsageUiState.Success -> {
                    val data = (state as DataUsageUiState.Success).data
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                    ) {
                        items(data) { app ->
                            AppUsageRow(app)
                        }
                    }
                }

                is DataUsageUiState.Empty -> {
                    Text(
                        "No recent network data found.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 10.dp),
                        color = Color.Gray
                    )
                }

                is DataUsageUiState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()) {
                        Text(
                            "Error: ${(state as DataUsageUiState.Error).message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if ((state as DataUsageUiState.Error).message.contains("Usage Access")) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { PermissionUtils.openUsageAccessSettings(context) }) {
                                Text("Grant Usage Access")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DataLimitBanner(
    totalBytes: Long,
    limitMb: Long,
    onSetLimit: () -> Unit
) {
    val limitBytes = limitMb * 1_000_000L
    val progress = if (limitMb > 0) (totalBytes.toFloat() / limitBytes).coerceIn(0f, 1f) else 0f
    val progressColor = when {
        progress >= 1f -> Color(0xFFFF3366)
        progress >= 0.8f -> Color(0xFFFF9800)
        else -> Color(0xFF00B3FF)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Monthly Data Usage", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(
                        if (limitMb > 0)
                            "${DataLimitManager.formatBytes(totalBytes)} / ${limitMb}MB"
                        else
                            DataLimitManager.formatBytes(totalBytes),
                        fontSize = 12.sp, color = Color.Gray
                    )
                }
                IconButton(onClick = onSetLimit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Set Limit",
                        tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (limitMb > 0) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = progressColor,
                    trackColor = progressColor.copy(alpha = 0.15f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${(progress * 100).toInt()}% used",
                    fontSize = 11.sp,
                    color = progressColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
