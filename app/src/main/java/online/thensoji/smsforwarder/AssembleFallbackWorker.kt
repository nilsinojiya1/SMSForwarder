package online.thensoji.smsforwarder

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import online.thensoji.smsforwarder.data.ForwardedMessage
import online.thensoji.smsforwarder.data.SmsPartDao
import online.thensoji.smsforwarder.repository.MessageRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltWorker
class AssembleFallbackWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val smsPartDao: SmsPartDao,
    private val repository: MessageRepository
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "AssembleFallbackWorker"
    }

    override suspend fun doWork(): Result {
        val sender = inputData.getString("sender") ?: return Result.failure()
        val refNumber = inputData.getInt("refNumber", -1)
        val simSlot = inputData.getInt("simSlot", -1)
        val timestamp = inputData.getLong("timestamp", System.currentTimeMillis())

        if (refNumber == -1) return Result.failure()

        val parts = smsPartDao.getPartsForRef(sender, refNumber)
        if (parts.isEmpty()) {
            // Already assembled and cleaned up by main receiver flow
            return Result.success()
        }

        Log.w(TAG, "Fallback assembly triggered for $sender (ref: $refNumber, parts received: ${parts.size}/${parts.firstOrNull()?.totalParts ?: "?"})")

        val fullBody = parts.sortedBy { it.partIndex }.joinToString("") { it.partBody }
        val dateString = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

        val message = """
            New SMS Received
            From: $sender
            SIM Slot: ${if (simSlot >= 0) simSlot + 1 else "Unknown"}
            Time: $dateString

            $fullBody
        """.trimIndent()

        val forwarded = ForwardedMessage(
            sender = sender,
            body = message,
            timestamp = timestamp,
            isSent = false,
            telegramMessageId = null
        )

        val id = repository.insertMessage(forwarded)
        smsPartDao.deletePartsForRef(sender, refNumber)

        val input = Data.Builder()
            .putLong("messageId", id)
            .build()
        val work = OneTimeWorkRequestBuilder<SendWorker>()
            .setInputData(input)
            .build()
        WorkManager.getInstance(applicationContext).enqueue(work)

        return Result.success()
    }
}

