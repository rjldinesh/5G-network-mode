package net.peaksoftstudios.fiveg.networkmode.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import net.peaksoftstudios.fiveg.MainActivity
import net.peaksoftstudios.fiveg.networkmode.R


class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "onMessageReceived: $remoteMessage")

        val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title ?: getString(R.string.app_name)
        val body = remoteMessage.data["body"] ?: remoteMessage.notification?.body ?: "No message"

        Log.d(TAG, "FCM Message - Title: $title, Body: $body")
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "fcm_channel_01"
        val channelName = "General Notifications"

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // ✅ Create channel (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications from LTE Mode App"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // ✅ Prepare click action
        val intent = Intent(this, MainActivity::class.java).apply {
           Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("title", title)
            putExtra("message", body)
        }

        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        // ✅ Build notification
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(resultPendingIntent)
            .setVibrate(longArrayOf(0, 300, 200, 300))

        // ✅ Check Android 13+ notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "Notification permission not granted.")
                return
            }
        }

        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), builder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token: $token")
    }

    companion object Companion {
        private const val TAG = "MyFirebaseMessagingService"
    }
}
