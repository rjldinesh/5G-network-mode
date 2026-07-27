package net.peaksoftstudios.fiveg.networkmode.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Restarts the network monitor foreground service after a reboot, but only if the
 * user had it switched on before the device restarted (NetworkMonitorService.isEnabled).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED &&
            NetworkMonitorService.isEnabled(context)
        ) {
            NetworkMonitorService.start(context)
        }
    }
}
