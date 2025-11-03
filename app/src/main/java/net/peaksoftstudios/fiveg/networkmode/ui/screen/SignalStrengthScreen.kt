package net.peaksoftstudios.fiveg.networkmode.ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat


@Composable
fun SignalStrengthScreen() {
    val context = LocalContext.current
    val telephonyManager =
        remember { context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }

    var signalDbm by remember { mutableStateOf("—") }
    var signalLevel by remember { mutableStateOf("—") }
    var networkType by remember { mutableStateOf("Unknown") }
    var carrierName by remember { mutableStateOf("—") }
    var simInfo by remember { mutableStateOf("—") }
    var cellId by remember { mutableStateOf("—") }
    var dataState by remember { mutableStateOf("—") }

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launcher for READ_PHONE_STATE
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> permissionGranted = granted }

    // Ask permission if not granted
    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        }
    }

    // Telephony listener
    DisposableEffect(permissionGranted) {
        if (permissionGranted) {
            val listener = object : PhoneStateListener() {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                    super.onSignalStrengthsChanged(signalStrength)

                    try {
                        val level = signalStrength?.level ?: 0
                        signalLevel = when (level) {
                            4 -> "Excellent"
                            3 -> "Good"
                            2 -> "Fair"
                            1 -> "Poor"
                            else -> "No Signal"
                        }

                        val asu = signalStrength?.gsmSignalStrength ?: 99
                        signalDbm = if (asu != 99) "${-113 + 2 * asu} dBm" else "N/A"

                        // Network type
                        networkType = when (telephonyManager.networkType) {
                            TelephonyManager.NETWORK_TYPE_LTE -> "4G / LTE"
                            TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
                            TelephonyManager.NETWORK_TYPE_HSPA,
                            TelephonyManager.NETWORK_TYPE_HSPAP -> "3G / HSPA"
                            TelephonyManager.NETWORK_TYPE_EDGE -> "2G / EDGE"
                            TelephonyManager.NETWORK_TYPE_GPRS -> "2G / GPRS"
                            else -> "Unknown"
                        }

                        // Carrier and SIM info — safe and compliant
                        val simStateText = getSimStateText(telephonyManager.simState)
                        val simOperator = telephonyManager.simOperator
                        val mcc = if (simOperator?.length ?: 0 >= 3) simOperator?.substring(0, 3) else "N/A"
                        val mnc = if (simOperator?.length ?: 0 >= 5) simOperator?.substring(3) else "N/A"
                        val simCountry = telephonyManager.simCountryIso?.uppercase() ?: "N/A"
                        val carrier = telephonyManager.networkOperatorName ?: "Unknown"

                        simInfo = buildString {
                            appendLine("SIM State: $simStateText")
                            appendLine("Carrier: $carrier")
                            appendLine("Country: $simCountry")
                            appendLine("MCC: $mcc | MNC: $mnc")
                        }

                        // Cell ID (requires location permission)
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            val cellInfoList = telephonyManager.allCellInfo
                            val lteInfo = cellInfoList?.filterIsInstance<CellInfoLte>()?.firstOrNull()
                            cellId = lteInfo?.cellIdentity?.ci?.toString() ?: "N/A"
                        } else {
                            cellId = "Location permission not granted"
                        }

                        // Data connection state
                        dataState = when (telephonyManager.dataState) {
                            TelephonyManager.DATA_CONNECTED -> "Connected"
                            TelephonyManager.DATA_CONNECTING -> "Connecting"
                            TelephonyManager.DATA_DISCONNECTED -> "Disconnected"
                            else -> "Unknown"
                        }

                        carrierName = carrier
                    } catch (e: SecurityException) {
                        signalDbm = "Permission Required"
                    }
                }
            }

            telephonyManager.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
            onDispose { telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE) }
        }

        onDispose { }
    }

    // ---------- UI -----------
    if (!permissionGranted) {
        PhoneStatePermissionRequestView { permissionLauncher.launch(Manifest.permission.READ_PHONE_STATE) }
    } else {
        SignalInfoView(
            signalDbm = signalDbm,
            signalLevel = signalLevel,
            networkType = networkType,
            carrierName = carrierName,
            simInfo = simInfo,
            cellId = cellId,
            dataState = dataState
        )
    }
}

@Composable
private fun SignalInfoView(
    signalDbm: String,
    signalLevel: String,
    networkType: String,
    carrierName: String,
    simInfo: String,
    cellId: String,
    dataState: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {

        InfoRow("Signal (dBm)", signalDbm)
        InfoRow("Signal Quality", signalLevel)
        InfoRow("Network Type", networkType)
        InfoRow("Carrier", carrierName)
        InfoRow("Cell ID", cellId)
        InfoRow("Data Connection", dataState)
        InfoRow("SIM Details", simInfo)
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary))
    }
}

@Composable
private fun PhoneStatePermissionRequestView(onGrant: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Permission Required",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Phone state permission is needed to show signal information.",
                fontSize = 14.sp
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onGrant) { Text("Grant Permission") }
        }
    }
}

private fun getSimStateText(state: Int): String = when (state) {
    TelephonyManager.SIM_STATE_READY -> "Ready"
    TelephonyManager.SIM_STATE_ABSENT -> "Absent"
    TelephonyManager.SIM_STATE_PIN_REQUIRED -> "PIN Required"
    TelephonyManager.SIM_STATE_PUK_REQUIRED -> "PUK Required"
    TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "Network Locked"
    else -> "Unknown"
}
