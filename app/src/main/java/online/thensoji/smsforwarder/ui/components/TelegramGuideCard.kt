package online.thensoji.smsforwarder.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TelegramGuideCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "How to get credentials:",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "1. Open Telegram and search for @BotFather.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "2. Create a new bot with /newbot to receive your Bot Token.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "3. Send a message to @userinfobot to get your personal Chat ID.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "4. Start your bot by opening its chat and pressing 'Start'.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

