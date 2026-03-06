package online.thensoji.smsforwarder.network

import android.content.Context
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class TelegramSender(private val client: OkHttpClient, private val context: Context) {

    fun sendMessage(botToken: String, chatId: String, message: String): Pair<Boolean, String?> {
        val url = "https://api.telegram.org/bot$botToken/sendMessage"
        return try {
            val jsonBody = JSONObject()
            jsonBody.put("chat_id", chatId)
            jsonBody.put("text", message)

            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Log.d("TelegramSender", "Message forwarded successfully")
                // ideally parse response to get message id
                Pair(true, null)
            } else {
                val body = response.body?.string()
                Log.e("TelegramSender", "Failed to forward message: $body")
                Pair(false, null)
            }
        } catch (e: Exception) {
            Log.e("TelegramSender", "Error forwarding message", e)
            Pair(false, null)
        }
    }
}
