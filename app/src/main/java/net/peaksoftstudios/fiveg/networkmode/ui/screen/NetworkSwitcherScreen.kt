package net.peaksoftstudios.fiveg.networkmode.ui.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.widget.Toast
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.peaksoftstudios.fiveg.networkmode.R
import net.peaksoftstudios.fiveg.networkmode.service.NetworkMonitorService


@Composable
fun NetworkSwitcherScreen() {
    val context = LocalContext.current
    var simList by remember { mutableStateOf<List<SubscriptionInfo>>(emptyList()) }
    var selectedSimIndex by remember { mutableStateOf(0) }
    var monitorEnabled by remember { mutableStateOf(NetworkMonitorService.isEnabled(context)) }


    // Launcher for requesting runtime permission
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // load SIMs once permission granted
            CoroutineScope(Dispatchers.Main).launch {
                simList = getActiveSimList(context)
            }
        } else {
            Toast.makeText(context, "Permission required to read SIM info", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        // Check permission before fetching
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_BASIC_PHONE_STATE // or READ_PRECISE_PHONE_STATE depending on need
        else
            Manifest.permission.READ_PHONE_STATE

        if (ContextCompat.checkSelfPermission(context, permission)
            == PackageManager.PERMISSION_GRANTED
        ) {
            simList = getActiveSimList(context)
        } else {
            permissionLauncher.launch(permission)
        }
    }




//    // Load active SIM info
//    LaunchedEffect(Unit) {
//        simList = getActiveSimList(context)
//
//        // ✅ Retry after small delay if initially empty
//        if (simList.isEmpty()) {
//            delay(2000)
//            simList = getActiveSimList(context)
//        }
//    }

    val selectedSim = simList.getOrNull(selectedSimIndex)

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { StatusHeroCard(selectedSim = selectedSim, simCount = simList.size) }

            if (simList.isNotEmpty()) {
                item { SectionLabel("Active SIM") }
                items(simList) { sim ->
                    val index = simList.indexOf(sim)
                    SimItem(sim, selected = index == selectedSimIndex) {
                        selectedSimIndex = index
                    }
                }
            } else {
                item { EmptySimCard() }
            }

            item { SectionLabel("Network mode") }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Switch between 4G and 5G",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Opens the system network panel where you can set the preferred network type for the selected SIM.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )
                        Button(
                            onClick = { openPhoneInfo(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.NetworkCell, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Open Network Settings", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            item { SectionLabel("Monitoring") }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconBadge(
                            icon = Icons.Default.NotificationsActive,
                            tint = if (monitorEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Network change alerts",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                "Get notified when your network drops or changes type.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = monitorEnabled,
                            onCheckedChange = { enabled ->
                                monitorEnabled = enabled
                                if (enabled) NetworkMonitorService.start(context)
                                else NetworkMonitorService.stop(context)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusHeroCard(selectedSim: SubscriptionInfo?, simCount: Int) {
    val primary = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = primary.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, primary.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.SignalCellularAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "5G / 4G Switcher",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = when {
                        selectedSim != null ->
                            "SIM ${selectedSim.simSlotIndex + 1} · ${selectedSim.carrierName}"
                        simCount == 0 -> "No SIM detected"
                        else -> "Select a SIM below"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun IconBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun EmptySimCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconBadge(
                icon = Icons.Default.SettingsInputAntenna,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "No active SIM cards detected",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                "Insert a SIM or grant phone permission to see your carriers here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun SimItem(sim: SubscriptionInfo, selected: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) primary.copy(alpha = 0.10f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) primary else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(
                icon = Icons.Default.SimCard,
                tint = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sim.carrierName?.toString() ?: "Unknown carrier",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "Slot ${sim.simSlotIndex + 1} · ${sim.number?.takeIf { it.isNotBlank() } ?: "No number"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = primary
                )
            }
        }
    }
}

/**
 * Tries to open the hidden RadioInfo screen first,
 * then falls back to official network settings or system settings.
 */

fun openPhoneInfo(context: Context) {
    val attempts = listOf(
        Intent(Intent.ACTION_MAIN).apply {
            setClassName("com.android.settings", "com.android.settings.RadioInfo")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        },
        Intent(Intent.ACTION_MAIN).apply {
            setClassName("com.android.phone", "com.android.phone.settings.RadioInfo")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        },
        Intent(Intent.ACTION_MAIN).apply {
            setClassName("com.android.settings", "com.android.settings.TestingSettings")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        },
        Intent("android.intent.action.MAIN").apply {
            setClassName(
                "com.android.settings",
                "com.android.settings.deviceinfo.PhoneInfoSettings"
            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        },
        Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        },
        Intent(Settings.ACTION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    )

    for (intent in attempts) {
        try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return
            }
        } catch (e: Exception) {
            // ignore and try next
        }
    }
    Toast.makeText(context, "Could not open Phone Info screen on this device", Toast.LENGTH_LONG)
        .show()
}

@Suppress("DEPRECATION")
suspend fun getActiveSimList(context: Context): List<SubscriptionInfo> =
    withContext(Dispatchers.IO) {
        try {
            val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                subManager.activeSubscriptionInfoList ?: emptyList()
            } else emptyList()
        } catch (e: SecurityException) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

