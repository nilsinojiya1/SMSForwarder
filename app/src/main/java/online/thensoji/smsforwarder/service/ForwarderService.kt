package online.thensoji.smsforwarder.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Telephony
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import online.thensoji.smsforwarder.util.NotificationHelper
import javax.inject.Inject

/**
 * Always-on foreground keep-alive service.
 *
 * Its job is twofold:
 *  1. Hold the process in the foreground bucket so aggressive OEM ROMs are far less likely to
 *     force-stop it (which would otherwise silence the [SmsReceiver] broadcast entirely).
 *  2. Register a [SmsContentObserver] on the Telephony SMS provider so incoming messages are
 *     captured directly from `content://sms` even if a broadcast is dropped.
 *
 * Declared as a {@code specialUse} foreground service since the ongoing task is continuous SMS
 * observation/forwarding rather than a bounded data sync.
 */
@AndroidEntryPoint
class ForwarderService : Service() {

    @Inject
    lateinit var smsContentObserver: SmsContentObserver

    private var observerRegistered = false

    companion object {
        private const val TAG = "SMSF ForwarderService"
        const val ACTION_STOP = "online.thensoji.smsforwarder.action.STOP_KEEPALIVE"
    }

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        registerObserver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Log.d(TAG, "Stop action received. Shutting down keep-alive service.")
            stopSelfSafely()
            return START_NOT_STICKY
        }
        // Re-assert foreground state on every (re)start, including system restarts.
        startAsForeground()
        registerObserver()
        // START_STICKY: ask the OS to recreate the service if it is killed.
        return START_STICKY
    }

    private fun startAsForeground() {
        val notification = NotificationHelper.buildKeepAliveNotification(this)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NotificationHelper.KEEP_ALIVE_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NotificationHelper.KEEP_ALIVE_NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enter foreground state", e)
        }
    }

    private fun registerObserver() {
        if (observerRegistered) return
        try {
            contentResolver.registerContentObserver(
                Telephony.Sms.CONTENT_URI,
                true,
                smsContentObserver
            )
            observerRegistered = true
            Log.d(TAG, "SmsContentObserver registered on Telephony SMS provider.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register SmsContentObserver", e)
        }
    }

    private fun unregisterObserver() {
        if (!observerRegistered) return
        try {
            contentResolver.unregisterContentObserver(smsContentObserver)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister SmsContentObserver: ${e.message}")
        } finally {
            observerRegistered = false
        }
    }

    private fun stopSelfSafely() {
        unregisterObserver()
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Log.w(TAG, "stopForeground failed: ${e.message}")
        }
        stopSelf()
    }

    override fun onDestroy() {
        unregisterObserver()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
