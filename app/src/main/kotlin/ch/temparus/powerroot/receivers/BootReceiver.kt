package ch.temparus.powerroot.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ch.temparus.powerroot.SharedMethods
import ch.temparus.powerroot.services.BatteryService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action && SharedMethods.isDevicePluggedIn(context)) {
            BatteryService.start(context)
        }
    }
}