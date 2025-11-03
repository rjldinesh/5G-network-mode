package net.peaksoftstudios.fiveg.networkmode.ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.CellInfoLte
import android.telephony.TelephonyManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import net.peaksoftstudios.fiveg.networkmode.R


@Composable
fun CellTowerScreen() {
    val context = LocalContext.current
    val telephonyManager =
        remember { context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }

    var towerDetails by remember { mutableStateOf("Fetching tower information...") }
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> permissionGranted = granted }

    // Request permission if not granted
    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Load tower details safely for all API levels
    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            try {
                val cellInfoList = telephonyManager.allCellInfo
                if (cellInfoList.isNullOrEmpty()) {
                    towerDetails = "No active cell tower information available."
                } else {
                    val infoBuilder = StringBuilder()
                    cellInfoList.take(3).forEachIndexed { index, cell ->
                        infoBuilder.appendLine("Tower ${index + 1}:")
                        when (cell) {
                            is CellInfoLte -> {
                                val id = cell.cellIdentity
                                infoBuilder.appendLine("  Type: LTE (4G)")
                                infoBuilder.appendLine("  CI: ${id.ci}")
                                infoBuilder.appendLine("  TAC: ${id.tac}")
                                infoBuilder.appendLine("  MCC: ${id.mcc}")
                                infoBuilder.appendLine("  MNC: ${id.mnc}")

                                // Safely access bandwidth (API 28+)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    infoBuilder.appendLine("  Bandwidth: ${id.bandwidth ?: "N/A"} kHz")
                                } else {
                                    infoBuilder.appendLine("  Bandwidth: N/A (not supported)")
                                }
                                infoBuilder.appendLine()
                            }

                            else -> {
                                // Safely check cell identity name (API 30+)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    val typeName =
                                        cell.cellIdentity.javaClass.simpleName ?: "Unknown"
                                    if (typeName.contains("Nr", ignoreCase = true)) {
                                        infoBuilder.appendLine("  Type: 5G NR")
                                        infoBuilder.appendLine("  Info restricted on this device")
                                        infoBuilder.appendLine()
                                    } else {
                                        infoBuilder.appendLine("  Type: $typeName")
                                        infoBuilder.appendLine("  Info not available for this network type")
                                        infoBuilder.appendLine()
                                    }
                                } else {
                                    infoBuilder.appendLine("  Type: Other / Unknown")
                                    infoBuilder.appendLine("  Detailed identity not supported on this Android version")
                                    infoBuilder.appendLine()
                                }
                            }
                        }
                    }
                    towerDetails = infoBuilder.toString()
                }
            } catch (e: SecurityException) {
                towerDetails = "Permission denied."
            } catch (e: Exception) {
                towerDetails = "Failed to retrieve tower information: ${e.message}"
            }
        } else {
            towerDetails = "Location permission required to access tower details."
        }
    }

    // UI
    if (!permissionGranted) { LocationPermissionRequestView {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
    } else {
        TowerInfoView(towerDetails)
    }
}

@Composable
private fun TowerInfoView(towerDetails: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Text(towerDetails, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun LocationPermissionRequestView(onGrant: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.permission_request_title),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.location_permission_needed_message),
                fontSize = 14.sp
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onGrant) { Text("Grant Permission") }
        }
    }
}
