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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Scaffold(
        topBar = {

        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header instructions
            Text(
                text = "Select your SIM slot and switch between available network modes (4G / 5G).",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Gray,
                    fontSize = 15.sp
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (simList.isNotEmpty()) {
                Text(
                    text = "Available SIMs:",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(simList) { sim ->
                        val isSelected = simList.indexOf(sim) == selectedSimIndex
                        SimItem(sim, isSelected) {
                            selectedSimIndex = simList.indexOf(sim)
                        }
                    }
                }


            } else {
                Text(
                    "No active SIM cards detected.",
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action button
            Button(
                onClick = {
                    openPhoneInfo(context)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.NetworkCell, contentDescription = "Network", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Network Settings", color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Use this option to manually change network preferences for the selected SIM.",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                modifier = Modifier.padding(horizontal = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Network Change Alerts",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        "Get notified when your network drops or changes type.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
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

@Composable
fun SimItem(sim: SubscriptionInfo, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (selected)
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        else
            BorderStroke(1.dp, Color.LightGray)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "SIM ${sim.simSlotIndex + 1}: ${sim.carrierName}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Number: ${sim.number ?: "N/A"}",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
            )
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

