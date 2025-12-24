package online.thensoji.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            // We don't need to do anything here as the SmsReceiver is already registered in the manifest
            // and will be active after the boot is completed.
        }
    }
}
