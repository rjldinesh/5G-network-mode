package net.peaksoftstudios.fiveg.networkmode.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NetworkWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget = NetworkWidget()

    companion object {
        // Shared, structured scope for widget update jobs instead of creating a
        // brand-new, uncancelable CoroutineScope on every call.
        private val updateScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun updateAll(context: Context) {
            updateScope.launch {
                NetworkWidget().updateAll(context)
            }
        }
    }
}
