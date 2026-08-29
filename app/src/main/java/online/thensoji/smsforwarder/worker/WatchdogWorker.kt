package online.thensoji.smsforwarder.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import online.thensoji.smsforwarder.repository.MessageRepository
import online.thensoji.smsforwarder.util.SmsInboxSyncHelper

@HiltWorker
class WatchdogWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val messageRepository: MessageRepository,
    private val inboxSyncHelper: SmsInboxSyncHelper
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "SMSF WatchdogWorker"
        const val WORK_NAME = "periodic_sms_watchdog"
    }

    override suspend fun doWork(): Result {
        return try {
            // 1. Reconcile with Android's system SMS inbox in case any broadcast was dropped or missed
            try {
                val newlyIngested = inboxSyncHelper.syncInboxMessages()
                if (newlyIngested > 0) {
                    Log.d(TAG, "Watchdog inbox scan successfully recovered $newlyIngested missed message(s).")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Inbox sync warning in Watchdog: ${e.message}")
            }

            // 2. Drain any unsent messages from Room database
            val unsent = messageRepository.getUnsentMessages()
            if (unsent.isNotEmpty()) {
                Log.d(TAG, "Watchdog found ${unsent.size} unsent message(s). Enqueuing SendWorker.")
                val workManager = WorkManager.getInstance(applicationContext)
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                for (msg in unsent) {
                    val input = Data.Builder()
                        .putLong("messageId", msg.id)
                        .build()
                    val work = OneTimeWorkRequestBuilder<SendWorker>()
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .setConstraints(constraints)
                        .setInputData(input)
                        .build()

                    workManager.enqueueUniqueWork(
                        "send_sms_${msg.id}",
                        ExistingWorkPolicy.KEEP,
                        work
                    )
                }
            } else {
                Log.d(TAG, "Watchdog check: 0 pending messages.")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in WatchdogWorker execution", e)
            Result.retry()
        }
    }
}
