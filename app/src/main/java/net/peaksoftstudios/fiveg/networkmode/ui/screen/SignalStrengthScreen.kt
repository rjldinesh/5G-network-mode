package net.peaksoftstudios.fiveg.networkmode.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import net.peaksoftstudios.fiveg.networkmode.manager.SignalEntry
import net.peaksoftstudios.fiveg.networkmode.manager.SignalHistoryManager
import net.peaksoftstudios.fiveg.networkmode.ui.components.AnimatedSignalStrengthGauge
import net.peaksoftstudios.fiveg.networkmode.ui.components.AppCard
import net.peaksoftstudios.fiveg.networkmode.ui.components.DetailRow
import net.peaksoftstudios.fiveg.networkmode.ui.components.PermissionCard
import net.peaksoftstudios.fiveg.networkmode.ui.components.PrimaryActionButton
import net.peaksoftstudios.fiveg.networkmode.ui.components.SectionEyebrow
import net.peaksoftstudios.fiveg.networkmode.ui.components.SignalHistoryChart
import net.peaksoftstudios.fiveg.networkmode.ui.components.StatTile
import net.peaksoftstudios.fiveg.networkmode.ui.components.signalQualityColor
import net.peaksoftstudios.fiveg.networkmode.ui.theme.MintDark
import net.peaksoftstudios.fiveg.networkmode.widget.WidgetKeys

/** Everything the signal screen shows, produced from one SignalStrength callback. */
data class SignalSnapshot(
    val dbm: Int? = null,
    val level: Int = 0,
    val networkType: String = "Unknown",
    val carrier: String = "—",
    val simState: String = "—",
    val country: String = "—",
    val mcc: String = "—",
    val mnc: String = "—",
    val cellId: String = "N/A",
    val dataState: String = "—"
)

@Composable
fun SignalStrengthScreen() {
    val context = LocalContext.current
    val telephonyManager =
        remember { context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }

    var snapshot by remember { mutableStateOf(SignalSnapshot()) }
    var historyEntries by remember { mutableStateOf(SignalHistoryManager.snapshot()) }
    // Throttles how often we persist to SharedPreferences / re-render the home screen
    // widget, since raw signal-strength callbacks can fire many times per second.
    var lastWidgetUpdateMs by remember { mutableLongStateOf(0L) }
    val widgetUpdateIntervalMs = 3_000L

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

    val phonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasPhonePermission = granted }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPhonePermission) phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        if (!hasLocationPermission) locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val onSignal: (TelephonyManager, SignalStrength) -> Unit = { tm, strength ->
        val next = readSignalSnapshot(context, tm, strength)
        snapshot = next
        val dbm = next.dbm
        if (dbm != null) {
            val now = System.currentTimeMillis()
            if (now - lastWidgetUpdateMs >= widgetUpdateIntervalMs) {
                lastWidgetUpdateMs = now
                SignalHistoryManager.record(dbm)
                historyEntries = SignalHistoryManager.snapshot()
                WidgetKeys.update(context, dbm, next.networkType, next.carrier, signalLevelText(next.level))
            }
        }
    }

    // ---- Signal listener with API fallback ----
    DisposableEffect(hasPhonePermission && hasLocationPermission) {
        if (hasPhonePermission && hasLocationPermission) {
            val subId = SubscriptionManager.getDefaultDataSubscriptionId()
            val tmForSim = telephonyManager.createForSubscriptionId(subId)
            val executor = ContextCompat.getMainExecutor(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
                    @SuppressLint("MissingPermission")
                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                        onSignal(tmForSim, signalStrength)
                    }
                }
                tmForSim.registerTelephonyCallback(executor, callback)
                onDispose { tmForSim.unregisterTelephonyCallback(callback) }
            } else {
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                        signalStrength?.let { onSignal(telephonyManager, it) }
                    }
                }
                @Suppress("DEPRECATION")
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
                onDispose { telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE) }
            }
        } else onDispose { }
    }

    if (!hasPhonePermission || !hasLocationPermission) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            PermissionCard(
                title = "Permissions required",
                message = "Phone and location access are needed to read signal strength and cell details.",
                icon = Icons.Filled.SignalCellularAlt
            ) {
                if (!hasPhonePermission) {
                    PrimaryActionButton(text = "Grant phone permission", height = 48.dp) {
                        phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                    }
                }
                if (!hasLocationPermission) {
                    PrimaryActionButton(text = "Grant location permission", height = 48.dp) {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }
            }
        }
    } else {
        SignalInfoView(snapshot = snapshot, historyEntries = historyEntries)
    }
}

/* ---------------- UI ---------------- */

@Composable
private fun SignalInfoView(snapshot: SignalSnapshot, historyEntries: List<SignalEntry>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SpeedTestScreen()

        AppCard(radius = 20.dp, contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp)) {
            SectionEyebrow("Signal strength")
            Spacer(Modifier.height(6.dp))
            AnimatedSignalStrengthGauge(
                signalLevel = snapshot.level,
                signalDbm = snapshot.dbm,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Network type", snapshot.networkType, Modifier.weight(1f))
                StatTile("Carrier", snapshot.carrier, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Cell ID", snapshot.cellId, Modifier.weight(1f))
                StatTile(
                    "Data",
                    snapshot.dataState,
                    Modifier.weight(1f),
                    valueColor = if (snapshot.dataState == "Connected") MintDark else MaterialTheme.colorScheme.primary
                )
            }
        }

        AppCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionEyebrow("SIM details")
                DetailRow("State", snapshot.simState)
                DetailRow("Country", snapshot.country)
                DetailRow("MCC / MNC", "${snapshot.mcc} / ${snapshot.mnc}")
                DetailRow("Quality", signalLevelText(snapshot.level), valueColor = signalQualityColor(snapshot.level))
            }
        }

        SignalHistoryChart(entries = historyEntries)
    }
}

/* ---------------- Helpers ---------------- */

private fun readSignalSnapshot(
    context: Context,
    tm: TelephonyManager,
    signalStrength: SignalStrength
): SignalSnapshot {
    var dbm: Int?
    var level: Int

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val list = signalStrength.cellSignalStrengths
        dbm = list.firstOrNull { it.dbm != CellInfo.UNAVAILABLE }?.dbm
        level = signalStrength.level
    } else {
        @Suppress("DEPRECATION")
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

    @Suppress("DEPRECATION")
    val rawType = tm.networkType
    val (gen, tech) = describeNetworkType(rawType)
    val network = if (tech.isBlank()) "Unknown" else "$gen / $tech"

    val simOperator = tm.simOperator
    val mcc = if ((simOperator?.length ?: 0) >= 3) simOperator.substring(0, 3) else "N/A"
    val mnc = if ((simOperator?.length ?: 0) >= 5) simOperator.substring(3) else "N/A"
    val country = tm.simCountryIso?.takeIf { it.isNotBlank() }?.uppercase() ?: "N/A"
    val carrier = tm.networkOperatorName?.takeIf { it.isNotBlank() } ?: "Unknown"

    var cell = "N/A"
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        try {
            val cellInfoList = tm.allCellInfo
            val lteInfo = cellInfoList?.filterIsInstance<CellInfoLte>()?.firstOrNull()
            cell = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val nrInfo = cellInfoList?.filterIsInstance<CellInfoNr>()?.firstOrNull()
                lteInfo?.cellIdentity?.ci?.toString()
                    ?: (nrInfo?.cellIdentity as? CellIdentityNr)?.nci?.toString()
                    ?: "N/A"
            } else {
                lteInfo?.cellIdentity?.ci?.toString() ?: "N/A"
            }
        } catch (e: SecurityException) {
            cell = "N/A"
        }
    }

    val dataState = when (tm.dataState) {
        TelephonyManager.DATA_CONNECTED -> "Connected"
        TelephonyManager.DATA_CONNECTING -> "Connecting"
        TelephonyManager.DATA_DISCONNECTED -> "Disconnected"
        else -> "Unknown"
    }

    return SignalSnapshot(
        dbm = dbm,
        level = level,
        networkType = network,
        carrier = carrier,
        simState = getSimStateText(tm.simState),
        country = country,
        mcc = mcc,
        mnc = mnc,
        cellId = cell,
        dataState = dataState
    )
}

private fun signalLevelText(level: Int): String = when (level) {
    4 -> "Excellent"
    3 -> "Good"
    2 -> "Fair"
    1 -> "Poor"
    else -> "No signal"
}

private fun getSimStateText(state: Int): String = when (state) {
    TelephonyManager.SIM_STATE_READY -> "Ready"
    TelephonyManager.SIM_STATE_ABSENT -> "Absent"
    TelephonyManager.SIM_STATE_PIN_REQUIRED -> "PIN required"
    TelephonyManager.SIM_STATE_PUK_REQUIRED -> "PUK required"
    TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "Network locked"
    else -> "Unknown"
}
