package online.thensoji.smsforwarder.ui.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import online.thensoji.smsforwarder.R
import online.thensoji.smsforwarder.ui.util.HapticFeedbackHelper
import online.thensoji.smsforwarder.ui.util.HapticType

@Composable
fun SecurityConsentDialog(
    isReviewMode: Boolean = false,
    onAccept: () -> Unit = {},
    onDecline: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = {
            if (isReviewMode) {
                onDismiss()
            } else {
                onDecline()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = isReviewMode,
            dismissOnClickOutside = isReviewMode,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with Shield Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.consent_dialog_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.consent_dialog_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Disclosure Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Section 1: SMS Access & Purpose
                    DisclosureSectionCard(
                        icon = Icons.Filled.Info,
                        title = stringResource(R.string.consent_sec1_title),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = stringResource(R.string.consent_sec1_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        BulletPoint(text = stringResource(R.string.consent_sec1_bullet1))
                        BulletPoint(text = stringResource(R.string.consent_sec1_bullet2))
                    }

                    // Section 2: Privacy & Transmission Security
                    DisclosureSectionCard(
                        icon = Icons.Filled.Lock,
                        title = stringResource(R.string.consent_sec2_title),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = stringResource(R.string.consent_sec2_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        BulletPoint(text = stringResource(R.string.consent_sec2_bullet1))
                        BulletPoint(text = stringResource(R.string.consent_sec2_bullet2))
                        BulletPoint(text = stringResource(R.string.consent_sec2_bullet3))
                    }

                    // Section 3: Strict Ethical Use & Anti-Stalkerware Policy
                    DisclosureSectionCard(
                        icon = Icons.Filled.Gavel,
                        title = stringResource(R.string.consent_sec3_title),
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                    ) {
                        Text(
                            text = stringResource(R.string.consent_sec3_body),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        BulletPoint(text = stringResource(R.string.consent_sec3_bullet1))
                        BulletPoint(text = stringResource(R.string.consent_sec3_bullet2))
                        BulletPoint(text = stringResource(R.string.consent_sec3_bullet3))
                    }

                    // Bottom Affirmation Notice
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.consent_affirmation),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(14.dp))

                // Symmetrical, Single-Line Action Buttons
                if (isReviewMode) {
                    val closeInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = {
                            HapticFeedbackHelper.performHaptic(context, view, HapticType.CLICK)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .pressScale(closeInteraction, scaleDown = 0.96f),
                        shape = RoundedCornerShape(12.dp),
                        interactionSource = closeInteraction
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.consent_action_close), fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val acceptInteraction = remember { MutableInteractionSource() }
                        Button(
                            onClick = {
                                HapticFeedbackHelper.performHaptic(context, view, HapticType.CLICK)
                                onAccept()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .pressScale(acceptInteraction, scaleDown = 0.96f),
                            shape = RoundedCornerShape(12.dp),
                            interactionSource = acceptInteraction
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.consent_action_agree), fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }

                        val declineInteraction = remember { MutableInteractionSource() }
                        OutlinedButton(
                            onClick = {
                                HapticFeedbackHelper.performHaptic(context, view, HapticType.CLICK)
                                onDecline()
                                (context as? Activity)?.finishAffinity()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .pressScale(declineInteraction, scaleDown = 0.96f),
                            shape = RoundedCornerShape(12.dp),
                            interactionSource = declineInteraction
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.consent_action_decline), fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DisclosureSectionCard(
    icon: ImageVector,
    title: String,
    containerColor: androidx.compose.ui.graphics.Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 6.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
