package online.thensoji.smsforwarder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import online.thensoji.smsforwarder.ui.screens.MainScreen
import online.thensoji.smsforwarder.ui.theme.SMSforwarderTheme
import online.thensoji.smsforwarder.util.HeartbeatManager

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HeartbeatManager.recordAppOpen(this)
        enableEdgeToEdge()
        setContent {
            SMSforwarderTheme {
                MainScreen()
            }
        }
    }
}