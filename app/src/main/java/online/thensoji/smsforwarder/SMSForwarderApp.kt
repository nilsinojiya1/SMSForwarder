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
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import online.thensoji.smsforwarder.repository.MessageRepository
import online.thensoji.smsforwarder.worker.SendWorker
import javax.inject.Inject

@HiltAndroidApp
class SMSForwarderApp : Application(), Configuration.Provider {

    companion object {
        private const val TAG = "SMSForwarderApp"
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var messageRepository: MessageRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        registerNetworkCallback()
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
                val unsent = messageRepository.getUnsentMessages()
                if (unsent.isNotEmpty()) {
                    Log.d(TAG, "Found ${unsent.size} pending unsent message(s). Enqueuing unique workers.")
                    val workManager = WorkManager.getInstance(applicationContext)
                    val constraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()

                    for (msg in unsent) {
                        val input = Data.Builder()
                            .putLong("messageId", msg.id)
                            .build()
                        val work = OneTimeWorkRequestBuilder<SendWorker>()
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
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error triggering pending messages dispatch", e)
            }
        }
    }
}
