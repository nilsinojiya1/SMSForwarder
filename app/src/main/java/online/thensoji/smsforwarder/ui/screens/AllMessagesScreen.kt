package online.thensoji.smsforwarder.ui.screens

import android.widget.Toast
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import online.thensoji.smsforwarder.ui.MessageViewModel
import online.thensoji.smsforwarder.ui.components.*
import online.thensoji.smsforwarder.ui.util.HapticFeedbackHelper
import online.thensoji.smsforwarder.ui.util.HapticType
import online.thensoji.smsforwarder.util.MessageFormatter
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AllMessagesScreen(
    viewModel: MessageViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val resendingIds by viewModel.resendingMessageIds.collectAsState()
    val isResendingAll by viewModel.isResendingAll.collectAsState()

    var selectedTab by remember { mutableStateOf(MessageFilterTab.ALL) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.refreshMessages()
    }

    val pendingList = remember(messages) { messages.filter { !it.isSent } }
    val sentList = remember(messages) { messages.filter { it.isSent } }
    val delayedList = remember(messages) { messages.filter { (it.delayMillis ?: 0L) >= 60_000L } }

    val filteredList = when (selectedTab) {
        MessageFilterTab.ALL -> messages
        MessageFilterTab.PENDING -> pendingList
        MessageFilterTab.SENT -> sentList
        MessageFilterTab.DELAYED -> delayedList
    }

    // Auto-scroll to the top whenever a new message is received or list changes
    val topMessageId = filteredList.firstOrNull()?.id
    LaunchedEffect(topMessageId) {
        if (topMessageId != null && listState.firstVisibleItemIndex <= 2) {
            listState.animateScrollToItem(0)
        }
    }

    val dateFormat = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header Row with interactive refresh loading spinner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "All Messages (${MessageFormatter.formatCompactNumber(messages.size)})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            val refreshInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = {
                    HapticFeedbackHelper.performHaptic(context, view, HapticType.CLICK)
                    viewModel.refreshMessages()
                },
                enabled = !isRefreshing,
                modifier = Modifier.pressScale(refreshInteraction, scaleDown = 0.88f),
                interactionSource = refreshInteraction
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontally Scrollable Filter Chips
        MessageFilterTabs(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            allCount = messages.size,
            pendingCount = pendingList.size,
            sentCount = sentList.size,
            delayedCount = delayedList.size
        )

        // Action banner if pending messages exist
        if (pendingList.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            PendingBanner(
                pendingCount = pendingList.size,
                isResending = isResendingAll,
                onSendNow = {
                    viewModel.resendAllPending(context)
                    Toast.makeText(context, "Retrying pending messages...", Toast.LENGTH_SHORT).show()
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content Area: Loading / Empty / List
        if (isLoading) {
            LoadingMessagesView(message = "Loading messages from database...")
        } else if (filteredList.isEmpty()) {
            EmptyMessagesView(selectedTab = selectedTab)
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredList, key = { it.id }) { msg ->
                    MessageCard(
                        msg = msg,
                        dateFormat = dateFormat,
                        isResending = resendingIds.contains(msg.id),
                        onResend = {
                            viewModel.resendMessage(context, msg.id)
                            Toast.makeText(context, "Retrying message...", Toast.LENGTH_SHORT).show()
                        },
                        onMarkSent = {
                            viewModel.markAsSent(msg.id)
                        },
                        onDelete = {
                            viewModel.deleteMessage(msg.id)
                        }
                    )
                }
            }
        }
    }
}
