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

/**
 * Notification used by the BatteryService
 */
class BatteryNotification(private val service: BatteryService) {

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
                .setContentTitle(service.getString(R.string.battery_notification_title))
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
                "Battery Charging Notification",
                NotificationManager.IMPORTANCE_LOW)

        // Configure the notification channel.
        notificationChannel.description = "Shows Battery Charging Information"
        notificationChannel.enableLights(false)
        notificationChannel.enableVibration(false)
        notificationManager.createNotificationChannel(notificationChannel)
    }

    /**
     * Update BatteryService notification
     *
     * @return true - if notification is shown; false - otherwise
     */
    fun update(): Boolean {
        mNotificationBuilder.mActions.clear()

        val state = service.getControlState()

        if (state == BatteryService.CONTROL_STATE_DISABLED || state == BatteryService.CONTROL_STATE_UNKNOWN) {
            service.stopForeground(true)
            return false
        }

        if (state == BatteryService.CONTROL_STATE_STOP_FORCED) {
            val chargePendingIntent = PendingIntent.getBroadcast(
                    service,
                    0,
                    Intent(BatteryService.ACTION_BATTERY_CHARGE),
                    PendingIntent.FLAG_UPDATE_CURRENT)
            mNotificationBuilder.addAction(NotificationCompat.Action(
                    0,
                    service.getString(R.string.battery_action_charge),
                    chargePendingIntent))
        }

        if (state == BatteryService.CONTROL_STATE_CHARGING || state == BatteryService.CONTROL_STATE_BOOST) {
            val chargePendingIntent = PendingIntent.getBroadcast(
                    service,
                    0,
                    Intent(BatteryService.ACTION_BATTERY_STOP),
                    PendingIntent.FLAG_UPDATE_CURRENT)
            mNotificationBuilder.addAction(NotificationCompat.Action(
                    0,
                    service.getString(R.string.battery_action_stop),
                    chargePendingIntent))
        }

        if (state == BatteryService.CONTROL_STATE_STOP || state == BatteryService.CONTROL_STATE_STOP_FORCED || state == BatteryService.CONTROL_STATE_CHARGING) {
            val boostPendingIntent = PendingIntent.getBroadcast(
                    service,
                    0,
                    Intent(BatteryService.ACTION_BATTERY_BOOST),
                    PendingIntent.FLAG_UPDATE_CURRENT)
            mNotificationBuilder.addAction(NotificationCompat.Action(
                    0,
                    service.getString(R.string.battery_action_boost_charge),
                    boostPendingIntent))
        }

        mNotificationBuilder.mContentText =
                when (state) {
                    BatteryService.CONTROL_STATE_CHARGING -> service.getString(R.string.battery_state_charging)
                    BatteryService.CONTROL_STATE_BOOST -> service.getString(R.string.battery_state_boost_charging)
                    BatteryService.CONTROL_STATE_STOP -> service.getString(R.string.battery_state_stopped)
                    BatteryService.CONTROL_STATE_STOP_FORCED -> service.getString(R.string.battery_state_interrupted)
                    else -> ""
                }

        service.startForeground(mNotificationId, mNotificationBuilder.build())
        return true
    }

    companion object {
        // configuration
        private const val NOTIFICATION_CHANNEL_ID: String = "PowerRootBatteryNotification"
        private const val NOTIFICATION_ICON: Int = R.drawable.ic_battery_charging_full_black_24dp
    }
}