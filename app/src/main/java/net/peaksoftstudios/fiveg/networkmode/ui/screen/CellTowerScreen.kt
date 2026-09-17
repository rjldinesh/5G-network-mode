package net.peaksoftstudios.fiveg.networkmode.ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import net.peaksoftstudios.fiveg.networkmode.R
import net.peaksoftstudios.fiveg.networkmode.ui.components.AppCard
import net.peaksoftstudios.fiveg.networkmode.ui.components.IconTile
import net.peaksoftstudios.fiveg.networkmode.ui.components.InfoNote
import net.peaksoftstudios.fiveg.networkmode.ui.components.PermissionCard
import net.peaksoftstudios.fiveg.networkmode.ui.components.PrimaryActionButton
import net.peaksoftstudios.fiveg.networkmode.ui.theme.Mint
import net.peaksoftstudios.fiveg.networkmode.ui.theme.MintDark
import net.peaksoftstudios.fiveg.networkmode.ui.theme.Navy

/** One cell as shown on the Tower screen. */
data class TowerInfo(
    val technology: String,          // "LTE", "5G NR", "GSM" …
    val isRegistered: Boolean,
    val fields: List<Pair<String, String>>,   // label → value, rendered in a 2-column grid
    val signalLabel: String?,        // e.g. "RSRP"
    val signalDbm: Int?,
    val note: String? = null         // shown instead of the grid when no identity is available
)

@Composable
fun CellTowerScreen() {
    val context = LocalContext.current
    val telephonyManager =
        remember { context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }

    var towers by remember { mutableStateOf<List<TowerInfo>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> permissionGranted = granted }

    LaunchedEffect(Unit) {
        if (!permissionGranted) permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    LaunchedEffect(permissionGranted, refreshTick) {
        if (!permissionGranted) return@LaunchedEffect
        try {
            val cells = telephonyManager.allCellInfo
            towers = cells.orEmpty().take(4).map { parseCell(it) }
            errorMessage = null
        } catch (e: SecurityException) {
            errorMessage = "Permission denied."
        } catch (e: Exception) {
            errorMessage = "Failed to retrieve tower information: ${e.message}"
        }
    }

    if (!permissionGranted) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            PermissionCard(
                title = stringResource(R.string.permission_request_title),
                message = stringResource(R.string.location_permission_needed_message),
                icon = Icons.Filled.LocationOn
            ) {
                PrimaryActionButton(text = "Grant permission", height = 48.dp) {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    when {
                        errorMessage != null -> errorMessage!!
                        towers.isEmpty() -> "No towers detected"
                        towers.size == 1 -> "1 tower detected"
                        else -> "${towers.size} towers detected"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { refreshTick++ }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = MintDark, modifier = Modifier.size(18.dp))
                    Text("Refresh", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.sp), color = MintDark)
                }
            }
        }

        if (towers.isEmpty() && errorMessage == null) {
            item {
                AppCard(radius = 20.dp, contentPadding = PaddingValues(18.dp)) {
                    InfoNote(
                        text = "No active cell tower information is available right now. Move to an area with coverage and tap Refresh.",
                        icon = Icons.Outlined.Info
                    )
                }
            }
        }

        items(towers.size) { index ->
            TowerCard(index = index, tower = towers[index])
        }
    }
}

@Composable
private fun TowerCard(index: Int, tower: TowerInfo) {
    AppCard(radius = 20.dp) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(
                icon = Icons.Filled.CellTower,
                background = if (tower.isRegistered) Navy else MaterialTheme.colorScheme.surfaceVariant,
                tint = if (tower.isRegistered) Mint else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Tower ${index + 1}", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${tower.technology} · ${if (tower.isRegistered) "Serving cell" else "Neighbour"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (tower.isRegistered) {
                Text(
                    "Registered",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        if (tower.note != null || tower.fields.isEmpty()) {
            InfoNote(
                text = tower.note ?: "Details aren’t available for this network type.",
                icon = Icons.Outlined.Info,
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 16.dp)
            )
        } else {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            val cells = tower.fields + listOfNotNull(
                tower.signalDbm?.let { (tower.signalLabel ?: "Signal") to "$it dBm" }
            )
            cells.chunked(2).forEachIndexed { rowIndex, pair ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    pair.forEachIndexed { colIndex, (label, value) ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 18.dp, vertical = 12.dp)
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                value,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
                                color = if (label == tower.signalLabel) MintDark else MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                        if (colIndex == 0 && pair.size == 2) {
                            Box(
                                Modifier
                                    .width(1.dp)
                                    .height(54.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
                if (rowIndex < cells.chunked(2).lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}

/* ---------------- Parsing ---------------- */

private fun Int.orNa(): String = if (this == CellInfo.UNAVAILABLE || this == Int.MAX_VALUE) "N/A" else toString()
private fun Long.orNa(): String = if (this == CellInfo.UNAVAILABLE_LONG || this == Long.MAX_VALUE) "N/A" else toString()
private fun Int.dbmOrNull(): Int? = if (this == CellInfo.UNAVAILABLE || this == Int.MAX_VALUE || this == Int.MIN_VALUE) null else this

@Suppress("DEPRECATION")
private fun parseCell(cell: CellInfo): TowerInfo {
    return when (cell) {
        is CellInfoLte -> {
            val id = cell.cellIdentity
            val mccMnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                "${id.mccString ?: "N/A"} / ${id.mncString ?: "N/A"}"
            else "${id.mcc.orNa()} / ${id.mnc.orNa()}"
            val fields = mutableListOf(
                "Cell ID" to id.ci.orNa(),
                "TAC" to id.tac.orNa(),
                "PCI" to id.pci.orNa(),
                "EARFCN" to id.earfcn.orNa(),
                "MCC / MNC" to mccMnc
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && id.bandwidth != CellInfo.UNAVAILABLE) {
                fields += "Bandwidth" to "${id.bandwidth / 1000} MHz"
            }
            val rsrp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) cell.cellSignalStrength.rsrp.dbmOrNull()
            else cell.cellSignalStrength.dbm.dbmOrNull()
            TowerInfo("LTE", cell.isRegistered, fields, "RSRP", rsrp)
        }

        else -> parseNonLte(cell)
    }
}

@Suppress("DEPRECATION")
private fun parseNonLte(cell: CellInfo): TowerInfo {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cell is CellInfoNr) {
        val id = cell.cellIdentity as? CellIdentityNr
        val ss = cell.cellSignalStrength as? CellSignalStrengthNr
        val fields = if (id != null) listOf(
            "NCI" to id.nci.orNa(),
            "TAC" to id.tac.orNa(),
            "PCI" to id.pci.orNa(),
            "NRARFCN" to id.nrarfcn.orNa(),
            "MCC / MNC" to "${id.mccString ?: "N/A"} / ${id.mncString ?: "N/A"}"
        ) else emptyList()
        return TowerInfo("5G NR", cell.isRegistered, fields, "SS-RSRP", ss?.ssRsrp?.dbmOrNull())
    }
    if (cell is CellInfoWcdma) {
        val id = cell.cellIdentity
        val mccMnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            "${id.mccString ?: "N/A"} / ${id.mncString ?: "N/A"}"
        else "${id.mcc.orNa()} / ${id.mnc.orNa()}"
        val fields = listOf(
            "Cell ID" to id.cid.orNa(),
            "LAC" to id.lac.orNa(),
            "PSC" to id.psc.orNa(),
            "UARFCN" to id.uarfcn.orNa(),
            "MCC / MNC" to mccMnc
        )
        return TowerInfo("3G / WCDMA", cell.isRegistered, fields, "RSCP", cell.cellSignalStrength.dbm.dbmOrNull())
    }
    if (cell is CellInfoGsm) {
        val id = cell.cellIdentity
        val mccMnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            "${id.mccString ?: "N/A"} / ${id.mncString ?: "N/A"}"
        else "${id.mcc.orNa()} / ${id.mnc.orNa()}"
        val fields = listOf(
            "Cell ID" to id.cid.orNa(),
            "LAC" to id.lac.orNa(),
            "ARFCN" to id.arfcn.orNa(),
            "BSIC" to id.bsic.orNa(),
            "MCC / MNC" to mccMnc
        )
        return TowerInfo("GSM", cell.isRegistered, fields, "RSSI", cell.cellSignalStrength.dbm.dbmOrNull())
    }
    if (cell is CellInfoCdma) {
        val id = cell.cellIdentity
        val fields = listOf(
            "Base station" to id.basestationId.orNa(),
            "Network ID" to id.networkId.orNa(),
            "System ID" to id.systemId.orNa()
        )
        return TowerInfo("CDMA", cell.isRegistered, fields, "RSSI", cell.cellSignalStrength.dbm.dbmOrNull())
    }
    val typeName = cell.javaClass.simpleName.removePrefix("CellInfo").ifBlank { "Unknown" }
    return TowerInfo(
        technology = typeName,
        isRegistered = cell.isRegistered,
        fields = emptyList(),
        signalLabel = null,
        signalDbm = null,
        note = "Details aren’t available for this network type."
    )
}
