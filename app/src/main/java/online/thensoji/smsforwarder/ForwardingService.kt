package online.thensoji.smsforwarder

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.IBinder
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import online.thensoji.smsforwarder.domain.model.SendResult
import online.thensoji.smsforwarder.domain.usecase.SendTelegramMessageUseCase
import javax.inject.Inject

@AndroidEntryPoint
class ForwardingService : Service() {

    @Inject
    lateinit var sendTelegramMessageUseCase: SendTelegramMessageUseCase

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val message = intent?.getStringExtra("message")
        if (message != null) {
            forwardMessageToTelegram(message)
        }
        return START_NOT_STICKY
    }

    private fun forwardMessageToTelegram(message: String) {
        val sharedPreferences = getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE)
        val botToken = sharedPreferences.getString("bot_token", null)
        val chatId = sharedPreferences.getString("chat_id", null)

        if (botToken.isNullOrEmpty() || chatId.isNullOrEmpty()) {
            Log.e("ForwardingService", "Bot token or chat ID is not set.")
            return
        }

        if (!isNetworkAvailable()) {
            Log.e("ForwardingService", "No internet connection.")
            return
        }

        coroutineScope.launch {
            try {
                when (val result = sendTelegramMessageUseCase(botToken, chatId, message)) {
                    is SendResult.Success -> {
                        Log.d("ForwardingService", "Message forwarded successfully. ID: ${result.telegramMessageId}")
                    }
                    is SendResult.Error -> {
                        Log.e("ForwardingService", "Failed to forward message: ${result.errorMessage}", result.throwable)
                    }
                }
            } catch (e: Exception) {
                Log.e("ForwardingService", "Error forwarding message", e)
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork =
            connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}