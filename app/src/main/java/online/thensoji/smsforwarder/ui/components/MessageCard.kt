package online.thensoji.smsforwarder.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import online.thensoji.smsforwarder.data.ForwardedMessage
import online.thensoji.smsforwarder.util.MessageFormatter
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageCard(
    msg: ForwardedMessage,
    dateFormat: SimpleDateFormat,
    onResend: () -> Unit,
    onMarkSent: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now = remember { System.currentTimeMillis() }
    val isDelayed = (msg.delayMillis ?: 0L) >= 60_000L
    val isPendingDelayed = !msg.isSent && (now - msg.timestamp) >= 60_000L

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Sender + Received Time (Responsive with overflow protection)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "From: ${msg.sender ?: "Unknown"}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dateFormat.format(Date(msg.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Status Badges Row
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Sent / Pending Badge
                if (msg.isSent) {
                    AssistChip(
                        onClick = {},
                        leadingIcon = {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Sent", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                } else {
                    AssistChip(
                        onClick = {},
                        leadingIcon = {
                            Icon(
                                Icons.Filled.HourglassEmpty,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Pending / Queued", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    )
                }

                // Delayed Badge
                if (isDelayed) {
                    val delayText = MessageFormatter.formatDelayDuration(msg.delayMillis ?: 0L)
                    AssistChip(
                        onClick = {},
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Delayed by $delayText", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    )
                } else if (isPendingDelayed) {
                    val pendingDelayText = MessageFormatter.formatDelayDuration(now - msg.timestamp)
                    AssistChip(
                        onClick = {},
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Offline for $pendingDelayText", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    )
                }

                // Telegram ID Badge
                if (!msg.telegramMessageId.isNullOrBlank()) {
                    AssistChip(
                        onClick = {},
                        label = { Text("ID #${msg.telegramMessageId}", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Error info if any
            if (!msg.errorMessage.isNullOrBlank() && !msg.isSent) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⚠️ ${msg.errorMessage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Message Body
            Text(
                text = msg.body,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Actions (FlowRow wrapped for small screens)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!msg.isSent) {
                    OutlinedButton(
                        onClick = onMarkSent,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Mark as Sent", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = onResend,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send Now", style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    OutlinedButton(
                        onClick = onResend,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resend", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

