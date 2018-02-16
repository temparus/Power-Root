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
import ch.temparus.powerroot.services.BatteryService
import ch.temparus.powerroot.services.SystemUpdaterService

/**
 * Notification used by the SystemUpdaterService
 */
class SystemUpdaterServiceNotification(private val service: SystemUpdaterService) {

    private var mNotificationBuilder: NotificationCompat.Builder
    private var mNotificationId = 1

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannelOreo(NOTIFICATION_CHANNEL_ID)
        }
        mNotificationBuilder = NotificationCompat.Builder(service, NOTIFICATION_CHANNEL_ID)

        val showActivityIntent = Intent(service, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
                service,
                0,
                showActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        mNotificationBuilder
                .setCategory(NotificationCompat.CATEGORY_SYSTEM)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setContentTitle(service.getString(R.string.system_updater_notification_title))
                .setSmallIcon(NOTIFICATION_ICON)
                .color = ContextCompat.getColor(service, R.color.colorPrimary)
        @Suppress("DEPRECATION")
        mNotificationBuilder.priority = Notification.PRIORITY_LOW
    }

    @TargetApi(26)
    private fun createNotificationChannelOreo(channelId: String) {
        val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = NotificationChannel(
                channelId,
                "System Updater Notification",
                NotificationManager.IMPORTANCE_LOW)

        // Configure the notification channel.
        notificationChannel.description = "Shows notifications about system updates"
        notificationChannel.enableLights(false)
        notificationChannel.enableVibration(false)
        notificationManager.createNotificationChannel(notificationChannel)
    }

    /**
     * Update SystemUpdaterService notification
     *
     * @return true - if notification is shown; false - otherwise
     */
    fun update(): Boolean {
        mNotificationBuilder.mActions.clear()

        val state = service.getUpdaterState()

        if (state == SystemUpdaterService.STATE_NO_UPDATE || state == SystemUpdaterService.STATE_UPDATE_INSTALLING) {
            service.stopForeground(true)
            return false
        }

        if (state == SystemUpdaterService.STATE_UPDATE_DOWNLOADING) {
            // TODO
        }

        if (state == SystemUpdaterService.STATE_UPDATE_READY) {
            // TODO
        }

        service.startForeground(mNotificationId, mNotificationBuilder.build())
        return true
    }

    companion object {
        // configuration
        private const val NOTIFICATION_CHANNEL_ID: String = "PowerRootSystemUpdaterNotification"
        private const val NOTIFICATION_ICON: Int = R.drawable.ic_system_update_black_24dp
    }
}