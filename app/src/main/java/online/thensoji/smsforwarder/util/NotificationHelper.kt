package online.thensoji.smsforwarder.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import online.thensoji.smsforwarder.R

object NotificationHelper {

    const val CHANNEL_ID = "sms_forwarding_channel"
    const val NOTIFICATION_ID = 1001
    const val KEEP_ALIVE_NOTIFICATION_ID = 1002

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = context.getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun buildForegroundInfo(context: Context): ForegroundInfo {
        createNotificationChannel(context)

        val notification: Notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_sending_title))
            .setContentText(context.getString(R.string.notification_active_desc))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    /** Persistent notification for the always-on keep-alive foreground service. */
    fun buildKeepAliveNotification(context: Context): Notification {
        createNotificationChannel(context)

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = if (launchIntent != null) {
            PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            null
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.keepalive_notification_title))
            .setContentText(context.getString(R.string.keepalive_notification_desc))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
