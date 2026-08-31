package online.thensoji.smsforwarder.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import online.thensoji.smsforwarder.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * At-a-glance background-health card for users who enabled the opt-in heartbeat. Surfaces the last
 * ping time, interval and battery-restriction status so failures are visible without opening logs.
 */
@Composable
fun HeartbeatHealthCard(
    lastSentMillis: Long,
    intervalMinutes: Long,
    batteryUnrestricted: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val cardModifier = if (onClick != null) {
        modifier.fillMaxWidth().bounceClickable(onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }

    val locale = LocalConfiguration.current.locales.get(0) ?: Locale.getDefault()
    val lastPing = if (lastSentMillis <= 0L) {
        stringResource(R.string.health_never)
    } else {
        SimpleDateFormat("MMM d, HH:mm", locale).format(Date(lastSentMillis))
    }

    val intervalLabel = when (intervalMinutes) {
        15L -> stringResource(R.string.dev_interval_15m)
        30L -> stringResource(R.string.dev_interval_30m)
        60L -> stringResource(R.string.dev_interval_1h)
        120L -> stringResource(R.string.dev_interval_2h)
        300L -> stringResource(R.string.dev_interval_5h)
        else -> stringResource(R.string.dev_interval_15m)
    }

    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.MonitorHeart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.health_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.health_heartbeat_active),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.health_last_ping, lastPing),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = stringResource(R.string.health_interval, intervalLabel),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (batteryUnrestricted) Icons.Filled.CheckCircle else Icons.Filled.BatteryAlert,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (batteryUnrestricted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (batteryUnrestricted)
                            stringResource(R.string.health_battery_ok)
                        else
                            stringResource(R.string.health_battery_warn),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (batteryUnrestricted)
                            MaterialTheme.colorScheme.onTertiaryContainer
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
