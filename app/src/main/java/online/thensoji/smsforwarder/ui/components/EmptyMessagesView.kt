package online.thensoji.smsforwarder.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import online.thensoji.smsforwarder.R

@Composable
fun EmptyMessagesView(
    selectedTab: MessageFilterTab,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = when (selectedTab) {
                    MessageFilterTab.PENDING -> Icons.Filled.CheckCircle
                    else -> Icons.Filled.Inbox
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = when (selectedTab) {
                    MessageFilterTab.ALL -> stringResource(R.string.empty_all_title)
                    MessageFilterTab.PENDING -> stringResource(R.string.empty_pending_title)
                    MessageFilterTab.SENT -> stringResource(R.string.empty_sent_title)
                    MessageFilterTab.DELAYED -> stringResource(R.string.empty_delayed_title)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (selectedTab) {
                    MessageFilterTab.ALL -> stringResource(R.string.empty_all_desc)
                    MessageFilterTab.PENDING -> stringResource(R.string.empty_pending_desc)
                    MessageFilterTab.SENT -> stringResource(R.string.empty_sent_desc)
                    MessageFilterTab.DELAYED -> stringResource(R.string.empty_delayed_desc)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

