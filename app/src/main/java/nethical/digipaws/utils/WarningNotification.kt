package nethical.digipaws.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import nethical.digipaws.R
import nethical.digipaws.ui.activity.WarningActivity


class WarningNotification(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "WarningNotificationChannel"
        private const val NOTIFICATION_ID = 1002
    }

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Warning Notifications",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Warning notifications"
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun sendNotification(title: String, desc: String) {

        val dialogIntent = Intent(context, WarningActivity::class.java)
        dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)


        val dialogPendingIntent = PendingIntent.getActivity(
            context, 0, dialogIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(desc)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setSilent(false)
            .addAction(
                R.drawable.baseline_warning_24,
                "+1 minute",
                dialogPendingIntent
            ) // Action button
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Click the Save button to store a value.")
            ) // Force expanded style
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}