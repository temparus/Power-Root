package ch.temparus.powerroot.services.notifications

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import ch.temparus.powerroot.MainActivity
import ch.temparus.powerroot.R

/**
 * Notification used by multiple services
 */
object DefaultNotification {

    const val NOTIFICATION_ID = 2
    private const val NOTIFICATION_CHANNEL_ID = "PowerRootDefaultServiceNotification"

    private var notification: Notification? = null

    /**
     * Get singleton instance of default notification
     * Used to share one notification between multiple services
     *
     * @return default notification
     */
    fun get(context: Context): Notification {
        if (notification == null) {
            notification = createNotification(context)
        }
        return notification as Notification
    }

    private fun createNotification(context: Context): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannelOreo(context, NOTIFICATION_CHANNEL_ID)
        }
        val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)

        val showActivityIntent = Intent(context, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
                context,
                0,
                showActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        notificationBuilder
                .setCategory(NotificationCompat.CATEGORY_SYSTEM)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText(context.getString(R.string.default_notification_title))
                .color = ContextCompat.getColor(context, R.color.colorPrimary)

        @Suppress("DEPRECATION")
        notificationBuilder.priority = Notification.PRIORITY_MIN

        return notificationBuilder.build()
    }

    @TargetApi(26)
    private fun createNotificationChannelOreo(context: Context, channelId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = NotificationChannel(
                channelId,
                "Default Notification Channel",
                NotificationManager.IMPORTANCE_MIN)

        // Configure the notification channel.
        notificationChannel.enableLights(false)
        notificationChannel.enableVibration(false)
        notificationManager.createNotificationChannel(notificationChannel)
    }
}