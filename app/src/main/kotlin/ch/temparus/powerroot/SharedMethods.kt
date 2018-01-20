package ch.temparus.powerroot

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import ch.temparus.powerroot.services.BatteryService
import eu.chainfire.libsuperuser.Shell
import java.io.File
import java.io.InputStream

@Suppress("MemberVisibilityCanPrivate")
/**
 * Utility methods shared between multiple classes
 */
object SharedMethods {

    private val rootShell: Shell.Interactive = Shell.Builder().setWantSTDERR(false).useSU().open()

    fun writeControlFile(file: String, content: String) {
        rootShell.addCommand(arrayOf("mount -o rw,remount $file", "echo \"$content\" > $file"))
    }

    fun readControlFile(file: String): String {
        val inputStream: InputStream = File(file).inputStream()
        return inputStream.bufferedReader().use { it.readText() }.trim()
    }

    fun isDevicePluggedIn(context: Context): Boolean {
        val batteryIntent = context.registerReceiver(null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val batteryStatus = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val connectionStatus = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val controlFileContent = Integer.parseInt(readControlFile(BatteryService.CONTROL_FILE))
        Log.d("SharedMethods", "BatteryStatus: $batteryStatus (Charging: ${BatteryManager.BATTERY_STATUS_DISCHARGING})")
        Log.d("SharedMethods", "ConnectionStatus: $connectionStatus")
        Log.d("SharedMethods", "ControlFile Content: $controlFileContent")

        return (batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING || connectionStatus > 0 || controlFileContent == 0)
    }
}