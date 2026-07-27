package net.peaksoftstudios.fiveg.networkmode.ui.screen.viewmodel

import android.annotation.SuppressLint
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.peaksoftstudios.fiveg.networkmode.manager.DataLimitManager
import net.peaksoftstudios.fiveg.networkmode.utils.PermissionUtils
import java.util.Calendar

/**
 * Represents a single app's data usage entry.
 */
data class AppDataUsage(
    val appName: String,
    val packageName: String,
    val downloadBytes: Long,
    val uploadBytes: Long
)

sealed class DataUsageUiState {
    object Loading : DataUsageUiState()
    data class Success(val data: List<AppDataUsage>) : DataUsageUiState()
    object Empty : DataUsageUiState()
    data class Error(val message: String) : DataUsageUiState()
}

class DataUsageViewModel(private val context: Context) : ViewModel() {

    private val _uiState = androidx.compose.runtime.mutableStateOf<DataUsageUiState>(DataUsageUiState.Loading)
    val uiState: androidx.compose.runtime.State<DataUsageUiState> get() = _uiState

    init {
        loadData()
    }

    fun loadData() {
        _uiState.value = DataUsageUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {

                // Check for permission
                if (!PermissionUtils.hasUsagePermission(context)) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = DataUsageUiState.Error("Usage Access permission not granted.")
                    }
                    return@launch
                }

                val data = getAppDataUsage(context)
                // Check data limit and fire notifications if needed
                val totalBytes = data.sumOf { it.downloadBytes + it.uploadBytes }
                DataLimitManager.checkAndNotify(context, totalBytes)
                withContext(Dispatchers.Main) {
                    _uiState.value =
                        if (data.isNotEmpty()) DataUsageUiState.Success(data)
                        else DataUsageUiState.Empty
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = DataUsageUiState.Error(e.message ?: "Failed to load data")
                }
            }
        }
    }
}


@SuppressLint("MissingPermission")
private fun getAppDataUsage(context: Context): List<AppDataUsage> {
    val networkStatsManager =
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
    val pm = context.packageManager
    val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
    val result = mutableListOf<AppDataUsage>()
    val end = System.currentTimeMillis()
    // Start of the current calendar month, so usage matches the "monthly" limit
    // shown/notified by DataLimitManager and the DataUsageScreen banner.
    val start = Calendar.getInstance().apply {
        timeInMillis = end
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    for (app in apps) {
        val uid = app.uid
        var rx = 0L
        var tx = 0L
        rx += getBytesForUid(networkStatsManager, ConnectivityManager.TYPE_MOBILE, uid, start, end).first
        tx += getBytesForUid(networkStatsManager, ConnectivityManager.TYPE_MOBILE, uid, start, end).second
        rx += getBytesForUid(networkStatsManager, ConnectivityManager.TYPE_WIFI, uid, start, end).first
        tx += getBytesForUid(networkStatsManager, ConnectivityManager.TYPE_WIFI, uid, start, end).second

        if (rx + tx > 0) {
            val label = pm.getApplicationLabel(app).toString()
            result.add(AppDataUsage(label, app.packageName, rx, tx))
        }
    }
    return result.sortedByDescending { it.downloadBytes + it.uploadBytes }
}

private fun getBytesForUid(
    nsm: NetworkStatsManager,
    type: Int,
    uid: Int,
    start: Long,
    end: Long
): Pair<Long, Long> {
    var rx = 0L
    var tx = 0L
    return try {
        val stats = nsm.queryDetailsForUid(type, null, start, end, uid)
        val bucket = NetworkStats.Bucket()
        while (stats.hasNextBucket()) {
            stats.getNextBucket(bucket)
            rx += bucket.rxBytes
            tx += bucket.txBytes
        }
        stats.close()
        Pair(rx, tx)
    } catch (e: Exception) {
        Pair(0L, 0L)
    }
}