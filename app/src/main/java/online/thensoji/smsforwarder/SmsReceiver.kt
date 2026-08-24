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
import online.thensoji.smsforwarder.data.ForwardedMessage
import online.thensoji.smsforwarder.data.SmsPart
import online.thensoji.smsforwarder.data.SmsPartDao
import online.thensoji.smsforwarder.repository.MessageRepository
import online.thensoji.smsforwarder.util.SmsPduParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    @Inject
    lateinit var repository: MessageRepository

    @Inject
    lateinit var smsPartDao: SmsPartDao

    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val pdus = intent.extras?.get("pdus") as? Array<*>

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

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    for (i in messages.indices) {
                        val sms = messages[i] ?: continue
                        val rawPdu = pdus?.getOrNull(i) as? ByteArray
                        val sender = sms.originatingAddress ?: "Unknown"
                        val timestamp = sms.timestampMillis
                        val body = sms.messageBody ?: ""

                        val concatInfo = SmsPduParser.getConcatInfo(sms, rawPdu)

                        if (concatInfo == null || concatInfo.totalParts <= 1) {
                            // Single-part standard SMS -> Forward immediately
                            val fullMessage = formatMessageText(sender, simSlot, timestamp, body)
                            val forwarded = ForwardedMessage(
                                sender = sender,
                                body = fullMessage,
                                timestamp = timestamp,
                                isSent = false,
                                telegramMessageId = null
                            )
                            val id = repository.insertMessage(forwarded)
                            enqueueSendWorker(context, id)
                        } else {
                            // Multi-part concatenated SMS
                            Log.d(
                                TAG,
                                "Received SMS part ${concatInfo.partIndex}/${concatInfo.totalParts} " +
                                        "(ref: ${concatInfo.refNumber}) from $sender"
                            )

                            smsPartDao.insertPart(
                                SmsPart(
                                    sender = sender,
                                    simSlot = simSlot,
                                    timestamp = timestamp,
                                    refNumber = concatInfo.refNumber,
                                    totalParts = concatInfo.totalParts,
                                    partIndex = concatInfo.partIndex,
                                    partBody = body
                                )
                            )

                            val existingParts = smsPartDao.getPartsForRef(sender, concatInfo.refNumber)

                            if (existingParts.size >= concatInfo.totalParts) {
                                // All parts received! Reassemble in correct sequence order
                                val fullBody = existingParts.sortedBy { it.partIndex }.joinToString("") { it.partBody }
                                val fullMessage = formatMessageText(sender, simSlot, timestamp, fullBody)
                                Log.d(TAG, "Reassembled complete SMS (${existingParts.size} parts): $fullMessage")

                                val forwarded = ForwardedMessage(
                                    sender = sender,
                                    body = fullMessage,
                                    timestamp = timestamp,
                                    isSent = false,
                                    telegramMessageId = null
                                )
                                val id = repository.insertMessage(forwarded)
                                smsPartDao.deletePartsForRef(sender, concatInfo.refNumber)
                                enqueueSendWorker(context, id)
                            } else {
                                // Missing parts, schedule delayed fallback worker (5 seconds)
                                enqueueFallbackAssemblyWorker(context, sender, concatInfo.refNumber, simSlot, timestamp)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing received SMS", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun formatMessageText(
        sender: String,
        simSlot: Int,
        timestamp: Long,
        body: String
    ): String {
        val dateString = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
        val slotText = if (simSlot >= 0) "${simSlot + 1}" else "Unknown"

        return """
            New SMS Received
            From: $sender
            SIM Slot: $slotText
            Time: $dateString

            $body
        """.trimIndent()
    }

    private fun enqueueSendWorker(context: Context, messageId: Long) {
        val input = Data.Builder()
            .putLong("messageId", messageId)
            .build()

        val work = OneTimeWorkRequestBuilder<SendWorker>()
            .setInputData(input)
            .build()

        WorkManager.getInstance(context).enqueue(work)
    }

    private fun enqueueFallbackAssemblyWorker(
        context: Context,
        sender: String,
        refNumber: Int,
        simSlot: Int,
        timestamp: Long
    ) {
        val input = Data.Builder()
            .putString("sender", sender)
            .putInt("refNumber", refNumber)
            .putInt("simSlot", simSlot)
            .putLong("timestamp", timestamp)
            .build()

        val work = OneTimeWorkRequestBuilder<AssembleFallbackWorker>()
            .setInputData(input)
            .setInitialDelay(5, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueue(work)
    }
}
