package online.thensoji.smsforwarder

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import online.thensoji.smsforwarder.repository.MessageRepository
import online.thensoji.smsforwarder.util.SmsInboxSyncHelper
import online.thensoji.smsforwarder.worker.SendWorker
import online.thensoji.smsforwarder.worker.WatchdogWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class SMSForwarderApp : Application(), Configuration.Provider {

    companion object {
        private const val TAG = "SMSF SMSForwarderApp"
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var messageRepository: MessageRepository

    @Inject
    lateinit var inboxSyncHelper: SmsInboxSyncHelper

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        schedulePeriodicWatchdog()
        registerNetworkCallback()
        triggerPendingMessagesDispatch()
    }

    private fun schedulePeriodicWatchdog() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val watchdogRequest = PeriodicWorkRequestBuilder<WatchdogWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                WatchdogWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                watchdogRequest
            )
            Log.d(TAG, "Periodic WatchdogWorker scheduled successfully with UPDATE policy.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule WatchdogWorker", e)
        }
    }

    private fun registerNetworkCallback() {
        try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val networkRequest = NetworkRequest.Builder().build()

            connectivityManager?.registerNetworkCallback(
                networkRequest,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        Log.d(TAG, "Network connection restored. Enqueuing pending messages...")
                        triggerPendingMessagesDispatch()
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }
    }

    private fun triggerPendingMessagesDispatch() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // First, sync any missing messages from device inbox
                try {
                    val syncedCount = inboxSyncHelper.syncInboxMessages()
                    Log.d(TAG, "[SMSF-DEBUG] Startup/Network trigger: inboxSyncHelper synced $syncedCount message(s).")
                } catch (e: Exception) {
                    Log.w(TAG, "[SMSF-DEBUG] Startup inbox sync warning: ${e.message}")
                }

                val unsent = messageRepository.getUnsentMessages()
                if (unsent.isNotEmpty()) {
                    Log.d(TAG, "[SMSF-DEBUG] triggerPendingMessagesDispatch: Found ${unsent.size} pending unsent message(s): [${unsent.joinToString { "#${it.id}" }}]. Enqueuing SendWorker(s)...")
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

                        // ExistingWorkPolicy.KEEP guarantees we never duplicate work or send twice
                        workManager.enqueueUniqueWork(
                            "send_sms_${msg.id}",
                            ExistingWorkPolicy.KEEP,
                            work
                        )
                    }
                } else {
                    Log.d(TAG, "[SMSF-DEBUG] triggerPendingMessagesDispatch: No pending unsent messages found.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "[SMSF-DEBUG] Error triggering pending messages dispatch", e)
            }
        }
    }
}
