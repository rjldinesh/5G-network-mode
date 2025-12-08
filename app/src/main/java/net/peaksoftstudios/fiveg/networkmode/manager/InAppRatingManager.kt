package net.peaksoftstudios.fiveg.networkmode.manager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class InAppRatingManager( private val activity: Activity,
                          private val context: Context) {

    suspend fun launchReviewWithFallback() {
        val manager = ReviewManagerFactory.create(activity)

        // Start a timer. If dialog doesn't show → fallback.
        val timeoutJob = CoroutineScope(Dispatchers.Main).launch {
            delay(1500)  // 1.5 seconds
            openPlayStore(context)
        }

        try {
            val request = manager.requestReviewFlow().await()
            manager.launchReviewFlow(activity, request).await()
            timeoutJob.cancel()  // Dialog shown → cancel fallback
        } catch (e: Exception) {
            e.printStackTrace()
            timeoutJob.cancel()
            openPlayStore(context) // Error → redirect to Play Store
        }
    }
    fun openPlayStore(context: Context) {
        val packageName = context.packageName
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName")
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

}
