package net.peaksoftstudios.fiveg.networkmode.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object DataLimitManager {

    private const val PREFS = "data_limit_prefs"
    private const val KEY_LIMIT_MB = "limit_mb"
    private const val KEY_NOTIFIED_80 = "notified_80"
    private const val KEY_NOTIFIED_100 = "notified_100"
    const val CHANNEL_ID = "data_limit_channel"
    private const val NOTIF_80_ID = 2001
    private const val NOTIF_100_ID = 2002

    /** Returns the stored limit in MB, or 0 if not set. */
    fun getLimit(context: Context): Long {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LIMIT_MB, 0L)
    }

    /** Saves the limit in MB and resets alert flags. */
    fun setLimit(context: Context, limitMb: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_LIMIT_MB, limitMb)
            .putBoolean(KEY_NOTIFIED_80, false)
            .putBoolean(KEY_NOTIFIED_100, false)
            .apply()
    }

    /** Call with the current month's usage in bytes. Fires notifications as needed. */
    fun checkAndNotify(context: Context, usedBytes: Long) {
        val limitMb = getLimit(context)
        if (limitMb <= 0) return

        val limitBytes = limitMb * 1_000_000L
        val usedPercent = (usedBytes.toDouble() / limitBytes * 100).toInt()

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        if (usedPercent >= 100 && !prefs.getBoolean(KEY_NOTIFIED_100, false)) {
            sendAlert(context, NOTIF_100_ID,
                "Data Limit Reached!",
                "You've used ${formatBytes(usedBytes)} — 100% of your ${limitMb}MB limit.")
            prefs.edit().putBoolean(KEY_NOTIFIED_100, true).apply()
        } else if (usedPercent >= 80 && !prefs.getBoolean(KEY_NOTIFIED_80, false)) {
            sendAlert(context, NOTIF_80_ID,
                "80% Data Limit Reached",
                "You've used ${formatBytes(usedBytes)} of your ${limitMb}MB limit.")
            prefs.edit().putBoolean(KEY_NOTIFIED_80, true).apply()
        }
    }

    private fun sendAlert(context: Context, id: Int, title: String, body: String) {
        createChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(id, notif)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID, "Data Usage Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply { description = "Alerts when you approach your data limit" }
                )
            }
        }
    }

    fun formatBytes(bytes: Long): String = when {
        bytes >= 1_000_000_000L -> "%.1f GB".format(bytes / 1_000_000_000.0)
        bytes >= 1_000_000L -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000L -> "%.1f KB".format(bytes / 1_000.0)
        else -> "$bytes B"
    }
}
