package online.thensoji.smsforwarder.util

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log

/**
 * Opens the manufacturer-specific "Auto-start" / "Background start" management screen so users can
 * whitelist the app on aggressive OEM ROMs (MIUI, ColorOS, FuntouchOS, EMUI, One UI, etc.).
 *
 * These OEM screens are not part of the Android SDK; they are reached through undocumented, ROM
 * specific Activity components that change between versions. We therefore try a prioritized list of
 * known components for the current manufacturer, then a generic list, and finally fall back to the
 * standard App Info page where the user can enable "Autostart" / "Allow background activity"
 * manually.
 */
object AutoStartHelper {

    private const val TAG = "SMSF AutoStartHelper"

    // Known OEM auto-start / background-start manager components, grouped loosely by manufacturer.
    private val AUTO_START_COMPONENTS = listOf(
        // Xiaomi / Redmi / POCO (MIUI)
        "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity",
        // Oppo / Realme (ColorOS)
        "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
        "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
        "com.oppo.safe" to "com.oppo.safe.permission.startup.StartupAppListActivity",
        "com.coloros.oppoguardelf" to "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity",
        // Vivo / iQOO (FuntouchOS / OriginOS)
        "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
        "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
        "com.iqoo.secure" to "com.iqoo.secure.safeguard.PurviewTabActivity",
        // Huawei / Honor (EMUI / MagicUI)
        "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
        "com.huawei.systemmanager" to "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity",
        "com.huawei.systemmanager" to "com.huawei.systemmanager.optimize.process.ProtectActivity",
        // Samsung (One UI) - device care / battery
        "com.samsung.android.lool" to "com.samsung.android.sm.ui.battery.BatteryActivity",
        "com.samsung.android.lool" to "com.samsung.android.sm.battery.ui.BatteryActivity",
        "com.samsung.android.sm_cn" to "com.samsung.android.sm.ui.battery.BatteryActivity",
        // OnePlus (OxygenOS)
        "com.oneplus.security" to "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity",
        // Letv
        "com.letv.android.letvsafe" to "com.letv.android.letvsafe.AutobootManageActivity",
        // Asus (ZenUI)
        "com.asus.mobilemanager" to "com.asus.mobilemanager.entry.FunctionActivity",
        "com.asus.mobilemanager" to "com.asus.mobilemanager.autostart.AutoStartActivity",
        // Meizu (Flyme)
        "com.meizu.safe" to "com.meizu.safe.security.SHOW_APPSEC",
        // Nokia (Evenwell)
        "com.evenwell.powersaving.g3" to "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity"
    )

    /**
     * Attempts to open a manufacturer auto-start manager. Returns true if an OEM screen was opened,
     * false if it fell back to the generic App Info page.
     */
    fun openAutoStartSettings(context: Context): Boolean {
        val pm = context.packageManager
        for ((pkg, cls) in AUTO_START_COMPONENTS) {
            val intent = Intent().apply {
                component = ComponentName(pkg, cls)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (isIntentResolvable(pm, intent)) {
                try {
                    context.startActivity(intent)
                    return true
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to open $pkg/$cls: ${e.message}")
                }
            }
        }
        openAppDetailsSettings(context)
        return false
    }

    private fun isIntentResolvable(pm: PackageManager, intent: Intent): Boolean {
        return try {
            @Suppress("DEPRECATION", "QueryPermissionsNeeded")
            pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    private fun openAppDetailsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Could not open app details settings: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected error opening app details settings: ${e.message}")
        }
    }
}
