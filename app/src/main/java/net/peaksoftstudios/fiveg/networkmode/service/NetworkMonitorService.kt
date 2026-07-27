package net.peaksoftstudios.fiveg.networkmode.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import net.peaksoftstudios.fiveg.MainActivity

class NetworkMonitorService : Service() {

    companion object {
        const val CHANNEL_MONITOR = "net_monitor_channel"
        const val CHANNEL_ALERT = "net_alert_channel"
        const val NOTIF_FOREGROUND_ID = 1001
        const val NOTIF_ALERT_ID = 1002

        private const val PREFS = "network_monitor_prefs"
        private const val KEY_ENABLED = "monitor_enabled"

        /** Whether the user has switched monitoring on (persisted, survives process death/reboot). */
        fun isEnabled(context: Context): Boolean =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

        fun start(context: Context) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_ENABLED, true).apply()
            val intent = Intent(context, NetworkMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_ENABLED, false).apply()
            context.stopService(Intent(context, NetworkMonitorService::class.java))
        }
    }

    private lateinit var connectivityManager: ConnectivityManager
    private var lastNetworkType: String? = null
    private var alertNotifCount = 0

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            val current = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> detectCellularGeneration(caps)
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                else -> "Other"
            }

            if (lastNetworkType != null && lastNetworkType != current) {
                sendNetworkChangeAlert(lastNetworkType!!, current)
            }
            lastNetworkType = current
        }

        override fun onLost(network: Network) {
            sendAlert(
                title = "No Internet Connection",
                body = "Your device lost network connectivity.",
                icon = android.R.drawable.ic_delete
            )
            lastNetworkType = null
        }

        override fun onAvailable(network: Network) {
            // Will be picked up by onCapabilitiesChanged
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannels()
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_FOREGROUND_ID, buildForegroundNotification())
        registerCallback()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerCallback() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching {
            connectivityManager.registerNetworkCallback(request, networkCallback)
        }
    }

    private fun detectCellularGeneration(caps: NetworkCapabilities): String {
        // Heuristic: 5G NR typically has very high bandwidth
        val downKbps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            caps.linkDownstreamBandwidthKbps else 0
        return when {
            downKbps >= 100_000 -> "5G NR"
            downKbps >= 10_000 -> "4G / LTE"
            downKbps >= 1_000 -> "3G"
            else -> "2G"
        }
    }

    private fun sendNetworkChangeAlert(from: String, to: String) {
        val isDrop = networkRank(to) < networkRank(from)
        val emoji = if (isDrop) "⚠️" else "✅"
        sendAlert(
            title = "$emoji Network Changed",
            body = "Switched from $from → $to",
            icon = android.R.drawable.ic_dialog_info
        )
    }

    private fun networkRank(type: String) = when (type) {
        "5G NR" -> 5
        "4G / LTE" -> 4
        "3G" -> 3
        "2G" -> 2
        "Wi-Fi" -> 6
        else -> 0
    }

    private fun sendAlert(title: String, body: String, icon: Int) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_ALERT)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapIntent)
            .build()
        nm.notify(NOTIF_ALERT_ID + alertNotifCount++, notif)
    }

    private fun buildForegroundNotification(): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_MONITOR)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Network Monitor Active")
            .setContentText("Watching for network changes")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setContentIntent(tapIntent)
            .build()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_MONITOR, "Network Monitor",
                    NotificationManager.IMPORTANCE_MIN
                ).apply { description = "Persistent monitoring notification" }
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ALERT, "Network Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Network change and disconnect alerts" }
            )
        }
    }
}
