package online.thensoji.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SubscriptionManager
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (smsMessage in messages) {
                val messageBody = smsMessage.messageBody
                val sender = smsMessage.originatingAddress
                val timestamp = smsMessage.timestampMillis
                val date = Date(timestamp)
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val dateString = format.format(date)

                val subscriptionId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    intent.getIntExtra("subscription", -1)
                } else {
                    -1
                }

                val simSlot = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                    val subInfo = subManager.getActiveSubscriptionInfo(subscriptionId)
                    subInfo?.simSlotIndex ?: -1
                } else {
                    -1
                }

                val message = """
                    New SMS Received
                    From: $sender
                    SIM Slot: ${simSlot + 1}
                    Time: $dateString

                    $messageBody
                """.trimIndent()

                Log.d("SmsReceiver", "Received SMS: $message")

                // Start service to forward the message
                val serviceIntent = Intent(context, ForwardingService::class.java)
                serviceIntent.putExtra("message", message)
                context.startService(serviceIntent)
            }
        }
    }
}
