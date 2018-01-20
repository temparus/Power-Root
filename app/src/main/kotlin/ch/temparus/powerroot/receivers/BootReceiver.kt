package ch.temparus.powerroot.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ch.temparus.powerroot.services.BatteryService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            BatteryService.start(context)
        }
    }
}