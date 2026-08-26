package online.thensoji.smsforwarder.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import online.thensoji.smsforwarder.R

@Composable
fun PermissionsCard(
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasPermissions) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (hasPermissions) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (hasPermissions) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasPermissions)
                        stringResource(R.string.perm_granted_title)
                    else
                        stringResource(R.string.perm_missing_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (hasPermissions)
                        stringResource(R.string.perm_granted_desc)
                    else
                        stringResource(R.string.perm_missing_desc),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (!hasPermissions) {
            val grantInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .pressScale(grantInteraction, scaleDown = 0.96f),
                interactionSource = grantInteraction
            ) {
                Text(
                    text = stringResource(R.string.perm_grant_button),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
