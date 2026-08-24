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

    fun formatCompactNumber(count: Int): String {
        return when {
            count >= 10_000_000 -> {
                val cr = count / 10_000_000.0
                if (cr % 1.0 == 0.0) "${cr.toInt()}cr" else String.format(Locale.US, "%.1fcr", cr)
            }
            count >= 100_000 -> {
                val lc = count / 100_000.0
                if (lc % 1.0 == 0.0) "${lc.toInt()}Lc" else String.format(Locale.US, "%.1fLc", lc)
            }
            count >= 1_000 -> {
                val k = count / 1000.0
                if (k % 1.0 == 0.0) "${k.toInt()}k" else String.format(Locale.US, "%.1fk", k)
            }
            else -> count.toString()
        }
    }

    fun formatDelayDuration(delayMillis: Long): String {
        val totalSeconds = (delayMillis / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> {
                val remHours = hours % 24
                if (remHours > 0) "${days}d ${remHours}h" else "${days}d"
            }
            hours > 0 -> {
                val remMins = minutes % 60
                if (remMins > 0) "${hours}h ${remMins}m" else "${hours}h"
            }
            minutes > 0 -> "${minutes}m"
            else -> "${totalSeconds}s"
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

    fun injectDelayTag(originalBody: String, delayMillis: Long): String {
        if (delayMillis < 60_000L) {
            return originalBody
        }
        val delayText = "⏳ [Delayed by ${formatDelayDuration(delayMillis)}]"
        val lines = originalBody.lines()
        return if (lines.isNotEmpty() && lines[0].startsWith("📱")) {
            // Insert delay tag right after the device header
            val firstLine = lines[0]
            val rest = lines.drop(1).joinToString("\n")
            "$firstLine\n$delayText\n$rest"
        } else {
            "$delayText\n$originalBody"
        }
    }
}
