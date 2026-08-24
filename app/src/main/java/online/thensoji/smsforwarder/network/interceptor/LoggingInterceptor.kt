package online.thensoji.smsforwarder.network.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import online.thensoji.smsforwarder.BuildConfig
import java.util.concurrent.TimeUnit

class LoggingInterceptor : Interceptor {

    companion object {
        private const val TAG = "TelegramHttpLog"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (!BuildConfig.DEBUG) {
            return chain.proceed(request)
        }

        Log.d(TAG, "--> ${request.method} ${request.url}")

        // Log request body if present
        request.body?.let { reqBody ->
            try {
                val buffer = Buffer()
                reqBody.writeTo(buffer)
                val requestContent = buffer.readUtf8()
                Log.d(TAG, "Request Body: $requestContent")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to log request body: ${e.message}")
            }
        }

        val startNs = System.nanoTime()
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            Log.e(TAG, "<-- HTTP FAILED: $e")
            throw e
        }

        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
        Log.d(TAG, "<-- ${response.code} ${response.message} ${response.request.url} (${tookMs}ms)")

        // Safely peek and log response body without consuming original stream
        try {
            val responseBody = response.peekBody(1024 * 1024)
            Log.d(TAG, "Response Body: ${responseBody.string()}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log response body: ${e.message}")
        }

        return response
    }
}

