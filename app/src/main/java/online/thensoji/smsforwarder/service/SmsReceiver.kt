package online.thensoji.smsforwarder.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import online.thensoji.smsforwarder.data.ForwardedMessage
import online.thensoji.smsforwarder.data.SmsPart
import online.thensoji.smsforwarder.data.SmsPartDao
import online.thensoji.smsforwarder.domain.model.SendResult
import online.thensoji.smsforwarder.domain.usecase.SendTelegramMessageUseCase
import online.thensoji.smsforwarder.repository.MessageRepository
import online.thensoji.smsforwarder.util.MessageFormatter
import online.thensoji.smsforwarder.util.SmsPduParser
import online.thensoji.smsforwarder.worker.AssembleFallbackWorker
import online.thensoji.smsforwarder.worker.SendWorker
import java.util.concurrent.TimeUnit

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
        private const val FALLBACK_DELAY_SECONDS = 30L
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SmsReceiverEntryPoint {
        fun getRepository(): MessageRepository
        fun getSmsPartDao(): SmsPartDao
        fun getSendTelegramMessageUseCase(): SendTelegramMessageUseCase
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION != action &&
            Telephony.Sms.Intents.SMS_DELIVER_ACTION != action
        ) {
            return
        }

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            SmsReceiverEntryPoint::class.java
        )
        val repository = entryPoint.getRepository()
        val smsPartDao = entryPoint.getSmsPartDao()
        val sendTelegramMessageUseCase = entryPoint.getSendTelegramMessageUseCase()

        val messages = try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract messages from intent", e)
            null
        }

        if (messages.isNullOrEmpty()) return

        // Defensive SIM slot extraction (Guards against SecurityException or null subManager)
        val simSlot = extractSimSlotSafely(context, intent)

        @Suppress("DEPRECATION")
        val pdus = try {
            intent.extras?.get("pdus") as? Array<*>
        } catch (e: Exception) {
            null
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
                        // Single-part standard SMS
                        val fullMessage = MessageFormatter.format(context, sender, simSlot, timestamp, body)
                        val forwarded = ForwardedMessage(
                            sender = sender,
                            body = fullMessage,
                            timestamp = timestamp,
                            isSent = false,
                            telegramMessageId = null
                        )
                        val id = repository.insertMessage(forwarded)
                        dispatchMessage(context, repository, sendTelegramMessageUseCase, id, fullMessage, timestamp)
                    } else {
                        // Multi-part concatenated SMS
                        Log.d(
                            TAG,
                            "Staging SMS part ${concatInfo.partIndex}/${concatInfo.totalParts} (ref: ${concatInfo.refNumber}) from $sender"
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
                            // All parts arrived! Assemble sequentially
                            val fullBody = existingParts.sortedBy { it.partIndex }.joinToString("") { it.partBody }
                            val fullMessage = MessageFormatter.format(context, sender, simSlot, timestamp, fullBody)
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
                            dispatchMessage(context, repository, sendTelegramMessageUseCase, id, fullMessage, timestamp)
                        } else {
                            // Schedule 30s fallback worker to flush partials if remaining parts are delayed
                            enqueueFallbackAssemblyWorker(context, sender, concatInfo.refNumber, simSlot, timestamp)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing incoming SMS in SmsReceiver", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun extractSimSlotSafely(context: Context, intent: Intent): Int {
        return try {
            val subscriptionId = intent.getIntExtra("subscription", -1)
            if (subscriptionId != -1) {
                val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                val subInfo = subManager?.getActiveSubscriptionInfo(subscriptionId)
                subInfo?.simSlotIndex ?: -1
            } else {
                -1
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "READ_PHONE_STATE permission not granted or restricted: ${e.message}")
            -1
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract SIM slot info: ${e.message}")
            -1
        }
    }

    private suspend fun dispatchMessage(
        context: Context,
        repository: MessageRepository,
        sendTelegramMessageUseCase: SendTelegramMessageUseCase,
        messageId: Long,
        messageBody: String,
        timestamp: Long
    ) {
        val prefs = context.getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE)
        val botToken = prefs.getString("bot_token", null)
        val chatId = prefs.getString("chat_id", null)

        var sentDirectly = false
        if (!botToken.isNullOrEmpty() && !chatId.isNullOrEmpty()) {
            try {
                val now = System.currentTimeMillis()
                val delayMillis = (now - timestamp).coerceAtLeast(0)
                val payload = if (delayMillis >= 60_000L) {
                    MessageFormatter.injectDelayTag(messageBody, delayMillis)
                } else {
                    messageBody
                }

                val sendResult = sendTelegramMessageUseCase(botToken, chatId, payload)
                if (sendResult is SendResult.Success) {
                    repository.markAsSent(
                        id = messageId,
                        telegramMessageId = sendResult.telegramMessageId,
                        sentTimestamp = now,
                        delayMillis = delayMillis
                    )
                    sentDirectly = true
                    Log.d(TAG, "Message $messageId forwarded directly to Telegram via SmsReceiver.")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Direct send in SmsReceiver failed, delegating to WorkManager: ${e.message}")
            }
        }

        // If direct send failed or device was offline, enqueue WorkManager for persistent retries
        if (!sentDirectly) {
            enqueueSendWorker(context, messageId)
        }
    }

    private fun enqueueSendWorker(context: Context, messageId: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val input = Data.Builder()
            .putLong("messageId", messageId)
            .build()

        val work = OneTimeWorkRequestBuilder<SendWorker>()
            .setConstraints(constraints)
            .setInputData(input)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "send_sms_$messageId",
            ExistingWorkPolicy.KEEP,
            work
        )
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
            .setInitialDelay(FALLBACK_DELAY_SECONDS, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueue(work)
    }
}

