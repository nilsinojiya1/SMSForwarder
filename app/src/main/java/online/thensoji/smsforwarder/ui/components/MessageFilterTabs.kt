package online.thensoji.smsforwarder.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import online.thensoji.smsforwarder.util.MessageFormatter

enum class MessageFilterTab(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    SENT("Sent"),
    DELAYED("Delayed")
}

@Composable
fun MessageFilterTabs(
    selectedTab: MessageFilterTab,
    onTabSelected: (MessageFilterTab) -> Unit,
    allCount: Int,
    pendingCount: Int,
    sentCount: Int,
    delayedCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedTab == MessageFilterTab.ALL,
            onClick = { onTabSelected(MessageFilterTab.ALL) },
            label = {
                Text(
                    text = "All (${MessageFormatter.formatCompactNumber(allCount)})",
                    maxLines = 1
                )
            }
        )
        FilterChip(
            selected = selectedTab == MessageFilterTab.PENDING,
            onClick = { onTabSelected(MessageFilterTab.PENDING) },
            label = {
                Text(
                    text = "Pending (${MessageFormatter.formatCompactNumber(pendingCount)})",
                    maxLines = 1
                )
            }
        )
        FilterChip(
            selected = selectedTab == MessageFilterTab.SENT,
            onClick = { onTabSelected(MessageFilterTab.SENT) },
            label = {
                Text(
                    text = "Sent (${MessageFormatter.formatCompactNumber(sentCount)})",
                    maxLines = 1
                )
            }
        )
        FilterChip(
            selected = selectedTab == MessageFilterTab.DELAYED,
            onClick = { onTabSelected(MessageFilterTab.DELAYED) },
            label = {
                Text(
                    text = "Delayed (${MessageFormatter.formatCompactNumber(delayedCount)})",
                    maxLines = 1
                )
            }
        )
    }
}

