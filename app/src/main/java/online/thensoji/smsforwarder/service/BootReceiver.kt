package online.thensoji.smsforwarder.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import online.thensoji.smsforwarder.repository.MessageRepository
import online.thensoji.smsforwarder.util.KeepAliveManager
import online.thensoji.smsforwarder.worker.SendWorker
import online.thensoji.smsforwarder.worker.WatchdogWorker
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SMSF BootReceiver"
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootReceiverEntryPoint {
        fun getMessageRepository(): MessageRepository
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Boot/System event received with action: $action")

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            BootReceiverEntryPoint::class.java
        )
        val messageRepository = entryPoint.getMessageRepository()

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 0. (Re)start the always-on keep-alive service. BOOT_COMPLETED / USER_PRESENT are
                //    allowed exemptions for starting a foreground service from the background.
                try {
                    KeepAliveManager.startIfEnabled(context)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not start keep-alive service on boot: ${e.message}")
                }

                // 1. Ensure Watchdog is scheduled
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val watchdogRequest = PeriodicWorkRequestBuilder<WatchdogWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()

                val workManager = WorkManager.getInstance(context)
                workManager.enqueueUniquePeriodicWork(
                    WatchdogWorker.WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    watchdogRequest
                )

                // 2. Dispatch all pending unsent messages from offline storage
                val unsent = messageRepository.getUnsentMessages()
                if (unsent.isNotEmpty()) {
                    Log.d(TAG, "Boot recovery found ${unsent.size} pending unsent message(s). Enqueuing workers.")
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
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in BootReceiver processing", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
