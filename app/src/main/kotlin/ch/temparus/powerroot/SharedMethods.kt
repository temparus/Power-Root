package ch.temparus.powerroot

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import ch.temparus.powerroot.services.BatteryService
import eu.chainfire.libsuperuser.Shell
import java.io.File
import java.io.FileInputStream
import java.io.IOException

@Suppress("MemberVisibilityCanBePrivate")
/**
 * Utility methods shared between multiple classes
 */
object SharedMethods {

    private val rootShell: Shell.Interactive = Shell.Builder().setWantSTDERR(false).useSU().open()
    private var powerSupplyPathList: List<String>? = null

    fun writeControlFile(file: String, content: String) {
        rootShell.addCommand(arrayOf("mount -o rw,remount $file", "echo \"$content\" > $file"))
    }

    fun readControlFile(file: String): List<String> {
        val content: List<String>
        try {
            val inputStream = FileInputStream(File(file))
            content = inputStream.bufferedReader().use { it.readText() }.split('\n')
            inputStream.close()
        } catch (ex: IOException) {
            return listOf()
        }
        return content
    }

    fun executeRootCommand(command: String) {
        rootShell.addCommand(command)
    }

    fun executeRootCommand(command: String, code: Int,
                            onCommandResultListener: Shell.OnCommandResultListener) {
        rootShell.addCommand(command, code, onCommandResultListener)
    }

    fun isDevicePluggedIn(context: Context): Boolean {
        val batteryIntent = context.registerReceiver(null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val batteryStatus = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val connectionStatus = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

        return (batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
                connectionStatus > 0 || hasExternalPowerSupply())
    }

    private fun hasExternalPowerSupply(): Boolean {
        if (powerSupplyPathList == null) {
            val powerSupplyDirectory = BatteryService.POWER_SUPPLY_DIRECTORY

            try {
                val directoryContent = Shell.SU.run("find -L $powerSupplyDirectory -maxdepth 1 -type d")
                if (!directoryContent.isEmpty() && directoryContent[0] == powerSupplyDirectory) {
                    directoryContent.removeAt(0)
                }
                powerSupplyPathList = directoryContent
            } catch(e: Exception) {
                return true
            }
        }

        powerSupplyPathList?.forEach {
            if (!it.contains("battery")) {
                val content = readControlFile("$it/present").joinToString("")
                if (content.isNotBlank() && Integer.parseInt(content) == 1) {
                    return true
                }
            }
        }
        return false
    }
}