package online.thensoji.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import online.thensoji.smsforwarder.data.ForwardedMessage
import online.thensoji.smsforwarder.repository.MessageRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: MessageRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            // Group message parts by sender + timestamp to reassemble multipart SMS
            val grouped = messages.groupBy { sms ->
                val sender = sms.originatingAddress ?: ""
                val ts = sms.timestampMillis
                "${sender}_$ts"
            }

            for ((_, parts) in grouped) {
                if (parts.isEmpty()) continue

                val first = parts[0]
                val sender = first.originatingAddress
                val timestamp = first.timestampMillis
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

                // Concatenate all parts' bodies
                val messageBody = parts.joinToString(separator = "") { it.messageBody }

                val message = """
                    New SMS Received
                    From: $sender
                    SIM Slot: ${simSlot + 1}
                    Time: $dateString

                    $messageBody
                """.trimIndent()

                Log.d("SmsReceiver", "Received SMS (reassembled): $message")

                // Persist message in Room and enqueue worker to send it
                val groupingId = "${sender}_$timestamp"
                val forwarded = ForwardedMessage(
                    sender = sender,
                    body = message,
                    timestamp = timestamp,
                    isSent = false,
                    telegramMessageId = null,
                    partsGroupingId = groupingId
                )

                CoroutineScope(Dispatchers.IO).launch {
                    val id = repository.insertMessage(forwarded)

                    // enqueue worker to send specific message id
                    val input = Data.Builder()
                        .putLong("messageId", id)
                        .build()

                    val work = OneTimeWorkRequestBuilder<SendWorker>()
                        .setInputData(input)
                        .build()

                    WorkManager.getInstance(context).enqueue(work)
                }
            }
        }
    }
}
