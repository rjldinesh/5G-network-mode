package net.peaksoftstudios.fiveg.networkmode.ui.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.peaksoftstudios.fiveg.networkmode.service.NetworkMonitorService
import net.peaksoftstudios.fiveg.networkmode.ui.components.AppCard
import net.peaksoftstudios.fiveg.networkmode.ui.components.HeroCaption
import net.peaksoftstudios.fiveg.networkmode.ui.components.HeroCard
import net.peaksoftstudios.fiveg.networkmode.ui.components.HeroEyebrow
import net.peaksoftstudios.fiveg.networkmode.ui.components.HeroPill
import net.peaksoftstudios.fiveg.networkmode.ui.components.IconTile
import net.peaksoftstudios.fiveg.networkmode.ui.components.PrimaryActionButton
import net.peaksoftstudios.fiveg.networkmode.ui.components.SectionEyebrow
import net.peaksoftstudios.fiveg.networkmode.ui.theme.Amber
import net.peaksoftstudios.fiveg.networkmode.ui.theme.Danger
import net.peaksoftstudios.fiveg.networkmode.ui.theme.LavenderOnNavy
import net.peaksoftstudios.fiveg.networkmode.ui.theme.Mint

/** Snapshot of the currently active radio, shown in the hero card. */
data class NetworkSnapshot(
    val generation: String,   // "5G", "4G", "3G", "2G" or "—"
    val technology: String,   // "LTE", "NR", "HSPA" … or "" when unknown
    val carrier: String,
    val dataState: Int        // TelephonyManager.DATA_*
)

@Composable
fun NetworkSwitcherScreen() {
    val context = LocalContext.current
    var simList by remember { mutableStateOf<List<SubscriptionInfo>>(emptyList()) }
    var slotCount by remember { mutableIntStateOf(1) }
    var selectedSimIndex by remember { mutableIntStateOf(0) }
    var monitorEnabled by remember { mutableStateOf(NetworkMonitorService.isEnabled(context)) }
    var snapshot by remember { mutableStateOf<NetworkSnapshot?>(null) }
    var hasPhonePermission by remember { mutableStateOf(hasPhoneStatePermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPhonePermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Permission required to read SIM info", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPhonePermission) permissionLauncher.launch(phoneStatePermission())
    }

    // Load SIMs once permission is available, then keep the hero card fresh.
    LaunchedEffect(hasPhonePermission) {
        if (!hasPhonePermission) return@LaunchedEffect
        simList = getActiveSimList(context)
        slotCount = getSlotCount(context).coerceAtLeast(simList.size).coerceAtLeast(1)
        while (true) {
            snapshot = readNetworkSnapshot(context, simList.getOrNull(selectedSimIndex))
            delay(4_000)
        }
    }

    LaunchedEffect(selectedSimIndex, simList) {
        if (hasPhonePermission) snapshot = readNetworkSnapshot(context, simList.getOrNull(selectedSimIndex))
    }

    val selectedSim = simList.getOrNull(selectedSimIndex)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { CurrentModeHero(snapshot = snapshot, selectedSim = selectedSim) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionEyebrow("Available SIMs", modifier = Modifier.padding(horizontal = 4.dp))
                if (simList.isEmpty() && !hasPhonePermission) {
                    AppCard(contentPadding = PaddingValues(16.dp), onClick = { permissionLauncher.launch(phoneStatePermission()) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconTile(icon = Icons.Default.SimCard, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Phone permission needed", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp))
                                Text("Tap to allow reading SIM details.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                simList.forEachIndexed { index, sim ->
                    SimItem(sim = sim, selected = index == selectedSimIndex) { selectedSimIndex = index }
                }
                val usedSlots = simList.map { it.simSlotIndex }.toSet()
                for (slot in 0 until slotCount) {
                    if (slot !in usedSlots) EmptySlotItem(slot = slot)
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryActionButton(
                    text = "Open network settings",
                    icon = Icons.Default.SettingsInputAntenna,
                    onClick = { openPhoneInfo(context) }
                )
                Text(
                    "Change the preferred network type for the selected SIM.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )
            }
        }

        item {
            AppCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(icon = Icons.Default.NotificationsActive)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Network change alerts", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp))
                        Text(
                            "Notify me when the network drops or changes type.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = monitorEnabled,
                        onCheckedChange = { enabled ->
                            monitorEnabled = enabled
                            if (enabled) NetworkMonitorService.start(context)
                            else NetworkMonitorService.stop(context)
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Mint,
                            checkedThumbColor = Color.White,
                            checkedBorderColor = Color.Transparent,
                            uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                            uncheckedThumbColor = Color.White,
                            uncheckedBorderColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentModeHero(snapshot: NetworkSnapshot?, selectedSim: SubscriptionInfo?) {
    val (statusText, statusDot) = when (snapshot?.dataState) {
        TelephonyManager.DATA_CONNECTED -> "Connected" to Mint
        TelephonyManager.DATA_CONNECTING -> "Connecting" to Amber
        TelephonyManager.DATA_DISCONNECTED -> "Disconnected" to Danger
        else -> "Unknown" to LavenderOnNavy
    }
    val carrier = snapshot?.carrier?.takeIf { it.isNotBlank() }
        ?: selectedSim?.carrierName?.toString()
        ?: "No SIM"
    val subtitle = listOfNotNull(snapshot?.technology?.takeIf { it.isNotBlank() }, carrier).joinToString(" · ")

    HeroCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeroEyebrow("Current mode")
            HeroPill(text = statusText, dotColor = statusDot)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                snapshot?.generation ?: "—",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = LavenderOnNavy,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        HeroCaption("Choose a SIM, then open your phone’s network settings to switch between 4G and 5G.")
    }
}

@Composable
fun SimItem(sim: SubscriptionInfo, selected: Boolean, onClick: () -> Unit) {
    val mintTint = MaterialTheme.colorScheme.onSecondaryContainer
    AppCard(
        borderColor = if (selected) Mint else MaterialTheme.colorScheme.outline,
        borderWidth = 2.dp,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(
                icon = Icons.Default.SimCard,
                background = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                tint = if (selected) mintTint else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "SIM ${sim.simSlotIndex + 1} · ${sim.carrierName?.toString()?.takeIf { it.isNotBlank() } ?: "Unknown carrier"}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
                    maxLines = 1
                )
                Text(
                    simNumberLabel(sim),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = mintTint, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun EmptySlotItem(slot: Int) {
    AppCard(
        borderWidth = 2.dp,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        modifier = Modifier.alpha(0.6f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon = Icons.Default.SimCard, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("SIM ${slot + 1}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp))
                Text("Empty slot", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun simNumberLabel(sim: SubscriptionInfo): String =
    sim.number?.takeIf { it.isNotBlank() } ?: "Number unavailable"

/* ---------------- Telephony helpers ---------------- */

private fun phoneStatePermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_BASIC_PHONE_STATE
    else Manifest.permission.READ_PHONE_STATE

private fun hasPhoneStatePermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, phoneStatePermission()) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

@Suppress("DEPRECATION")
private fun getSlotCount(context: Context): Int = try {
    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) tm.activeModemCount else tm.phoneCount
} catch (e: Exception) {
    1
}

@Suppress("DEPRECATION", "MissingPermission")
private fun readNetworkSnapshot(context: Context, sim: SubscriptionInfo?): NetworkSnapshot? = try {
    val base = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    val tm = if (sim != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) base.createForSubscriptionId(sim.subscriptionId) else base
    val type = try { tm.dataNetworkType } catch (e: SecurityException) { tm.networkType }
    val (gen, tech) = describeNetworkType(type)
    NetworkSnapshot(
        generation = gen,
        technology = tech,
        carrier = tm.networkOperatorName?.takeIf { it.isNotBlank() } ?: sim?.carrierName?.toString() ?: "",
        dataState = tm.dataState
    )
} catch (e: Exception) {
    null
}

/** Maps a TelephonyManager.NETWORK_TYPE_* constant to a generation + technology label. */
fun describeNetworkType(type: Int): Pair<String, String> = when (type) {
    TelephonyManager.NETWORK_TYPE_NR -> "5G" to "NR"
    TelephonyManager.NETWORK_TYPE_LTE -> "4G" to "LTE"
    TelephonyManager.NETWORK_TYPE_HSPAP -> "3G" to "HSPA+"
    TelephonyManager.NETWORK_TYPE_HSPA,
    TelephonyManager.NETWORK_TYPE_HSDPA,
    TelephonyManager.NETWORK_TYPE_HSUPA -> "3G" to "HSPA"
    TelephonyManager.NETWORK_TYPE_UMTS -> "3G" to "UMTS"
    TelephonyManager.NETWORK_TYPE_EVDO_0,
    TelephonyManager.NETWORK_TYPE_EVDO_A,
    TelephonyManager.NETWORK_TYPE_EVDO_B,
    TelephonyManager.NETWORK_TYPE_EHRPD -> "3G" to "EVDO"
    TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "3G" to "TD-SCDMA"
    TelephonyManager.NETWORK_TYPE_EDGE -> "2G" to "EDGE"
    TelephonyManager.NETWORK_TYPE_GPRS -> "2G" to "GPRS"
    TelephonyManager.NETWORK_TYPE_CDMA,
    TelephonyManager.NETWORK_TYPE_1xRTT -> "2G" to "CDMA"
    TelephonyManager.NETWORK_TYPE_GSM -> "2G" to "GSM"
    TelephonyManager.NETWORK_TYPE_IWLAN -> "Wi-Fi" to "IWLAN"
    else -> "—" to ""
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
