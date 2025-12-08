package net.peaksoftstudios.fiveg.networkmode.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
import net.peaksoftstudios.fiveg.networkmode.ui.components.AnimatedSignalStrengthGauge

@Composable
fun SignalStrengthScreen() {
    val context = LocalContext.current
    val telephonyManager =
        remember { context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }

    var signalDbm by remember { mutableStateOf("—") }
    var signalLevel by remember { mutableStateOf("—") }
    var signalLevelInt by remember { mutableIntStateOf(0) }
    var signalIntDbm by remember { mutableIntStateOf(0) }
    var networkType by remember { mutableStateOf("Unknown") }
    var carrierName by remember { mutableStateOf("—") }
    var simInfo by remember { mutableStateOf("—") }
    var cellId by remember { mutableStateOf("—") }
    var dataState by remember { mutableStateOf("—") }

    var hasPhonePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    // ---- Permission launchers ----
    val phonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasPhonePermission = granted }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

    // Request permissions on launch
    LaunchedEffect(Unit) {
        if (!hasPhonePermission) phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        if (!hasLocationPermission) locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // ---- Signal listener with API fallback ----
    DisposableEffect(hasPhonePermission && hasLocationPermission) {
        if (hasPhonePermission && hasLocationPermission) {
            val subId = SubscriptionManager.getDefaultDataSubscriptionId()
            val tmForSim = telephonyManager.createForSubscriptionId(subId)
            val executor = ContextCompat.getMainExecutor(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // ✅ Android 12+ modern API
                val callback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
                    @SuppressLint("MissingPermission")
                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                        updateSignalUI(context, tmForSim, signalStrength,
                            onUpdate = { dbm, level, net, carrier, sim, cell, state ->
                                signalIntDbm = dbm ?: 0
                                signalDbm = dbm?.let { "$it dBm" } ?: "N/A"
                                signalLevelInt = level
                                signalLevel = signalLevelText(level)
                                networkType = net
                                carrierName = carrier
                                simInfo = sim
                                cellId = cell
                                dataState = state
                            })
                    }
                }
                tmForSim.registerTelephonyCallback(executor, callback)
                onDispose { tmForSim.unregisterTelephonyCallback(callback) }
            } else {
                // ✅ Android 11 and below fallback
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                        signalStrength?.let {
                            updateSignalUI(context, telephonyManager, it,
                                onUpdate = { dbm, level, net, carrier, sim, cell, state ->
                                    signalIntDbm = dbm ?: 0
                                    signalDbm = dbm?.let { "$it dBm" } ?: "N/A"
                                    signalLevelInt = level
                                    signalLevel = signalLevelText(level)
                                    networkType = net
                                    carrierName = carrier
                                    simInfo = sim
                                    cellId = cell
                                    dataState = state
                                })
                        }
                    }
                }
                @Suppress("DEPRECATION")
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
                onDispose { telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE) }
            }
        } else onDispose { }
    }

    // ---- UI ----
    if (!hasPhonePermission || !hasLocationPermission) {
        PhoneStatePermissionRequestView(
            hasPhonePermission = hasPhonePermission,
            hasLocationPermission = hasLocationPermission,
            onGrantPhone = { phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE) },
            onGrantLocation = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
        )
    } else {
        SignalInfoView(
            signalDbm = signalDbm,
            signalLevel = signalLevel,
            singnalLevelInPercentage = signalLevelInt,
            singalInIntDbm = signalIntDbm,
            networkType = networkType,
            carrierName = carrierName,
            simInfo = simInfo,
            cellId = cellId,
            dataState = dataState
        )
    }
}

/* ---------------- Helper functions ---------------- */

private fun updateSignalUI(
    context: Context,
    tm: TelephonyManager,
    signalStrength: SignalStrength,
    onUpdate: (
        dbm: Int?, level: Int, network: String,
        carrier: String, simInfo: String, cellId: String, dataState: String
    ) -> Unit
) {
    var dbm: Int? = null
    var level = 0

    // --- Get dBm and level safely ---
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {       // API 29+
        val list = signalStrength.cellSignalStrengths
        dbm = list.firstOrNull { it.dbm != CellInfo.UNAVAILABLE }?.dbm
        level = signalStrength.level
    } else {                                                    // API 24–28
        val asu = signalStrength.gsmSignalStrength
        dbm = if (asu != 99) -113 + 2 * asu else null
        level = when {
            dbm == null -> 0
            dbm > -75 -> 4
            dbm > -90 -> 3
            dbm > -105 -> 2
            dbm > -120 -> 1
            else -> 0
        }
    }

    // --- Network type ---
    val network = when (tm.networkType) {
        TelephonyManager.NETWORK_TYPE_LTE -> "4G / LTE"
        TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
        TelephonyManager.NETWORK_TYPE_HSPA,
        TelephonyManager.NETWORK_TYPE_HSPAP -> "3G / HSPA"
        TelephonyManager.NETWORK_TYPE_EDGE -> "2G / EDGE"
        TelephonyManager.NETWORK_TYPE_GPRS -> "2G / GPRS"
        else -> "Unknown"
    }

    // --- SIM and carrier info ---
    val simStateText = getSimStateText(tm.simState)
    val simOperator = tm.simOperator
    val mcc = if ((simOperator?.length ?: 0) >= 3) simOperator?.substring(0, 3) else "N/A"
    val mnc = if ((simOperator?.length ?: 0) >= 5) simOperator?.substring(3) else "N/A"
    val simCountry = tm.simCountryIso?.uppercase() ?: "N/A"
    val carrier = tm.networkOperatorName ?: "Unknown"

    val simInfo = buildString {
        appendLine("SIM State: $simStateText")
        appendLine("Carrier: $carrier")
        appendLine("Country: $simCountry")
        appendLine("MCC: $mcc | MNC: $mnc")
    }

    // --- Cell ID (LTE for <29, NR for ≥29) ---
    var cell = "N/A"
    if (
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED
    ) {
        val cellInfoList = tm.allCellInfo
        val lteInfo = cellInfoList?.filterIsInstance<CellInfoLte>()?.firstOrNull()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val nrInfo = cellInfoList?.filterIsInstance<CellInfoNr>()?.firstOrNull()
            cell = lteInfo?.cellIdentity?.ci?.toString()
                ?: nrInfo?.cellIdentity?.let { (it as? CellIdentityNr)?.nci?.toString() }
                        ?: "N/A"
        } else {
            cell = lteInfo?.cellIdentity?.ci?.toString() ?: "N/A"
        }
    }

    // --- Data connection state ---
    val dataState = when (tm.dataState) {
        TelephonyManager.DATA_CONNECTED -> "Connected"
        TelephonyManager.DATA_CONNECTING -> "Connecting"
        TelephonyManager.DATA_DISCONNECTED -> "Disconnected"
        else -> "Unknown"
    }

    onUpdate(dbm, level, network, carrier, simInfo, cell, dataState)
}

private fun signalLevelText(level: Int): String = when (level) {
    4 -> "Excellent"
    3 -> "Good"
    2 -> "Fair"
    1 -> "Poor"
    else -> "No Signal"
}

/* ---------------- UI Composables ---------------- */

@Composable
private fun SignalInfoView(
    signalDbm: String,
    signalLevel: String,
    singnalLevelInPercentage: Int,
    singalInIntDbm: Int,
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
        AnimatedSignalStrengthGauge(signalLevel = singnalLevelInPercentage, signalDbm = singalInIntDbm)

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
        Text(title, style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground))
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary))
    }
}

@Composable
private fun PhoneStatePermissionRequestView(
    hasPhonePermission: Boolean,
    hasLocationPermission: Boolean,
    onGrantPhone: () -> Unit,
    onGrantLocation: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Permissions Required",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text("This app needs Phone and Location permissions to show signal details.")
            Spacer(Modifier.height(16.dp))
            if (!hasPhonePermission)
                Button(onClick = onGrantPhone) { Text("Grant Phone Permission") }
            Spacer(Modifier.height(8.dp))
            if (!hasLocationPermission)
                Button(onClick = onGrantLocation) { Text("Grant Location Permission") }
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
