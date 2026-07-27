package net.peaksoftstudios.fiveg.networkmode.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import net.peaksoftstudios.fiveg.MainActivity

// Keys used to pass live data from the main app into the widget
object WidgetKeys {
    const val PREFS_NAME = "widget_prefs"
    const val KEY_SIGNAL_DBM = "signal_dbm"
    const val KEY_NETWORK_TYPE = "network_type"
    const val KEY_CARRIER = "carrier"
    const val KEY_QUALITY = "signal_quality"

    fun update(context: Context, dbm: Int, networkType: String, carrier: String, quality: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_SIGNAL_DBM, dbm)
            .putString(KEY_NETWORK_TYPE, networkType)
            .putString(KEY_CARRIER, carrier)
            .putString(KEY_QUALITY, quality)
            .apply()
        NetworkWidgetReceiver.updateAll(context)
    }
}

class NetworkWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences(WidgetKeys.PREFS_NAME, Context.MODE_PRIVATE)
        val dbm = prefs.getInt(WidgetKeys.KEY_SIGNAL_DBM, 0)
        val networkType = prefs.getString(WidgetKeys.KEY_NETWORK_TYPE, "—") ?: "—"
        val carrier = prefs.getString(WidgetKeys.KEY_CARRIER, "—") ?: "—"
        val quality = prefs.getString(WidgetKeys.KEY_QUALITY, "—") ?: "—"

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color(0xFF0A1628)))
                        .padding(12.dp)
                        .clickable {
                            val intent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Network type badge
                    Text(
                        text = networkType,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF00B3FF)),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.height(4.dp))

                    // Signal dBm
                    Text(
                        text = if (dbm != 0) "$dbm dBm" else "No signal",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 14.sp
                        )
                    )
                    Spacer(GlanceModifier.height(2.dp))

                    // Quality
                    Text(
                        text = quality,
                        style = TextStyle(
                            color = ColorProvider(qualityColor(quality)),
                            fontSize = 12.sp
                        )
                    )
                    Spacer(GlanceModifier.height(2.dp))

                    // Carrier
                    Text(
                        text = carrier,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFAAAAAA)),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }

    private fun qualityColor(quality: String): Color = when (quality) {
        "Excellent" -> Color(0xFF00C853)
        "Good" -> Color(0xFF00B3FF)
        "Fair" -> Color(0xFFFF9800)
        "Poor", "No Signal" -> Color(0xFFFF3366)
        else -> Color.White
    }
}
