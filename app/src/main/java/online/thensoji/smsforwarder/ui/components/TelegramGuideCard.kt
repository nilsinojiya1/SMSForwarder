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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import online.thensoji.smsforwarder.R

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
                text = stringResource(R.string.settings_guide_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_guide_step1),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.settings_guide_step2),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.settings_guide_step3),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.settings_guide_step4),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

