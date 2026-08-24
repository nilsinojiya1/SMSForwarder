package online.thensoji.smsforwarder.util

import android.content.Context
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MessageFormatter {

    fun getDefaultDeviceName(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        val model = Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }
    }

    fun getDeviceName(context: Context): String {
        val prefs = context.getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE)
        val customName = prefs.getString("device_name", "")?.trim()
        return if (!customName.isNullOrEmpty()) {
            customName
        } else {
            getDefaultDeviceName()
        }
    }

    fun format(
        context: Context,
        sender: String,
        simSlot: Int,
        timestamp: Long,
        body: String
    ): String {
        val deviceName = getDeviceName(context)
        val dateString = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
        val slotText = if (simSlot >= 0) "${simSlot + 1}" else "Unknown"

        return """
            📱 [$deviceName]
            From: $sender
            SIM Slot: $slotText
            Time: $dateString

            $body
        """.trimIndent()
    }
}

