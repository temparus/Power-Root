package ch.temparus.powerroot.receivers

import android.widget.Toast
import android.content.Intent
import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.preference.PreferenceManager
import ch.temparus.powerroot.R
import ch.temparus.powerroot.services.ProximityService


class LockScreenAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)

        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(ProximityService.PREF_LOCK_SCREEN, true)
                .apply()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)

        Toast.makeText(context, R.string.removed_device_admin_rights, Toast.LENGTH_SHORT).show()
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(ProximityService.PREF_LOCK_SCREEN, false)
                .apply()
    }
}

