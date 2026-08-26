package online.thensoji.smsforwarder.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import online.thensoji.smsforwarder.R
import online.thensoji.smsforwarder.ui.util.HapticFeedbackHelper
import online.thensoji.smsforwarder.ui.util.HapticType
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
    val context = LocalContext.current
    val view = LocalView.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val allInteraction = remember { MutableInteractionSource() }
        FilterChip(
            selected = selectedTab == MessageFilterTab.ALL,
            onClick = {
                HapticFeedbackHelper.performHaptic(context, view, HapticType.TICK)
                onTabSelected(MessageFilterTab.ALL)
            },
            modifier = Modifier.pressScale(allInteraction, scaleDown = 0.94f),
            interactionSource = allInteraction,
            label = {
                Text(
                    text = stringResource(
                        R.string.filter_tab_all,
                        MessageFormatter.formatCompactNumber(allCount)
                    ),
                    maxLines = 1
                )
            }
        )

        val pendingInteraction = remember { MutableInteractionSource() }
        FilterChip(
            selected = selectedTab == MessageFilterTab.PENDING,
            onClick = {
                HapticFeedbackHelper.performHaptic(context, view, HapticType.TICK)
                onTabSelected(MessageFilterTab.PENDING)
            },
            modifier = Modifier.pressScale(pendingInteraction, scaleDown = 0.94f),
            interactionSource = pendingInteraction,
            label = {
                Text(
                    text = stringResource(
                        R.string.filter_tab_pending,
                        MessageFormatter.formatCompactNumber(pendingCount)
                    ),
                    maxLines = 1
                )
            }
        )

        val sentInteraction = remember { MutableInteractionSource() }
        FilterChip(
            selected = selectedTab == MessageFilterTab.SENT,
            onClick = {
                HapticFeedbackHelper.performHaptic(context, view, HapticType.TICK)
                onTabSelected(MessageFilterTab.SENT)
            },
            modifier = Modifier.pressScale(sentInteraction, scaleDown = 0.94f),
            interactionSource = sentInteraction,
            label = {
                Text(
                    text = stringResource(
                        R.string.filter_tab_sent,
                        MessageFormatter.formatCompactNumber(sentCount)
                    ),
                    maxLines = 1
                )
            }
        )

        val delayedInteraction = remember { MutableInteractionSource() }
        FilterChip(
            selected = selectedTab == MessageFilterTab.DELAYED,
            onClick = {
                HapticFeedbackHelper.performHaptic(context, view, HapticType.TICK)
                onTabSelected(MessageFilterTab.DELAYED)
            },
            modifier = Modifier.pressScale(delayedInteraction, scaleDown = 0.94f),
            interactionSource = delayedInteraction,
            label = {
                Text(
                    text = stringResource(
                        R.string.filter_tab_delayed,
                        MessageFormatter.formatCompactNumber(delayedCount)
                    ),
                    maxLines = 1
                )
            }
        )
    }
}
