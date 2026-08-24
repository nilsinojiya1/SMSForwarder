package online.thensoji.smsforwarder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import online.thensoji.smsforwarder.ui.screens.MainScreen
import online.thensoji.smsforwarder.ui.theme.SMSforwarderTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SMSforwarderTheme {
                MainScreen()
            }
        }
    }
}