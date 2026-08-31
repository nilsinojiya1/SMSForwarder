package online.thensoji.smsforwarder.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import online.thensoji.smsforwarder.R
import online.thensoji.smsforwarder.ui.MessageViewModel
import online.thensoji.smsforwarder.ui.components.pressScale
import online.thensoji.smsforwarder.ui.util.HapticFeedbackHelper
import online.thensoji.smsforwarder.ui.util.HapticType
import online.thensoji.smsforwarder.util.HeartbeatManager

@Composable
fun DeveloperScreen(
    viewModel: MessageViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var enabled by remember { mutableStateOf(HeartbeatManager.isEnabled(context)) }
    var token by remember { mutableStateOf(HeartbeatManager.getToken(context)) }
    var chatId by remember {
        mutableStateOf(
            context.getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE)
                .getString(HeartbeatManager.KEY_CHAT_ID, "") ?: ""
        )
    }
    var intervalMinutes by remember { mutableStateOf(HeartbeatManager.getIntervalMinutes(context)) }
    var isTokenVisible by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var lastSentLabel by remember { mutableStateOf(HeartbeatManager.formatLastSent(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.MonitorHeart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = stringResource(R.string.dev_heartbeat_section),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = stringResource(R.string.dev_heartbeat_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.dev_heartbeat_enable),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Switch(
                checked = enabled,
                onCheckedChange = {
                    HapticFeedbackHelper.performHaptic(context, view, HapticType.TICK)
                    enabled = it
                }
            )
        }

        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text(stringResource(R.string.dev_heartbeat_token_label)) },
            placeholder = { Text(stringResource(R.string.dev_heartbeat_token_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val toggleInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = {
                        HapticFeedbackHelper.performHaptic(context, view, HapticType.TICK)
                        isTokenVisible = !isTokenVisible
                    },
                    modifier = Modifier.pressScale(toggleInteraction, scaleDown = 0.88f),
                    interactionSource = toggleInteraction
                ) {
                    Icon(
                        imageVector = if (isTokenVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null
                    )
                }
            }
        )

        OutlinedTextField(
            value = chatId,
            onValueChange = { chatId = it },
            label = { Text(stringResource(R.string.dev_heartbeat_chat_label)) },
            placeholder = { Text(stringResource(R.string.dev_heartbeat_chat_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        // Interval selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = stringResource(R.string.dev_heartbeat_interval_label),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                HeartbeatManager.INTERVAL_OPTIONS.forEach { minutes ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = intervalMinutes == minutes,
                                onClick = {
                                    HapticFeedbackHelper.performHaptic(context, view, HapticType.TICK)
                                    intervalMinutes = minutes
                                }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = intervalMinutes == minutes,
                            onClick = {
                                HapticFeedbackHelper.performHaptic(context, view, HapticType.TICK)
                                intervalMinutes = minutes
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(HeartbeatManager.intervalLabel(context, minutes))
                    }
                }
            }
        }

        val saveInteraction = remember { MutableInteractionSource() }
        Button(
            onClick = {
                keyboardController?.hide()
                focusManager.clearFocus()
                HapticFeedbackHelper.performHaptic(context, view, HapticType.SUCCESS)
                HeartbeatManager.save(context, enabled, token, chatId, intervalMinutes)
                HeartbeatManager.applySchedule(context)
                Toast.makeText(context, context.getString(R.string.dev_saved), Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(saveInteraction, scaleDown = 0.96f),
            interactionSource = saveInteraction
        ) {
            Text(stringResource(R.string.dev_save), fontWeight = FontWeight.SemiBold)
        }

        val testInteraction = remember { MutableInteractionSource() }
        OutlinedButton(
            onClick = {
                keyboardController?.hide()
                focusManager.clearFocus()
                HapticFeedbackHelper.performHaptic(context, view, HapticType.CLICK)
                val trimmedToken = token.trim()
                val effectiveChat = chatId.trim().ifEmpty { HeartbeatManager.getChatId(context) }
                if (trimmedToken.isEmpty() || effectiveChat.isEmpty()) {
                    HapticFeedbackHelper.performHaptic(context, view, HapticType.ERROR)
                    Toast.makeText(context, context.getString(R.string.dev_test_empty), Toast.LENGTH_SHORT).show()
                    return@OutlinedButton
                }
                // Persist so buildPingMessage/interval reflect the current inputs.
                HeartbeatManager.save(context, enabled, trimmedToken, chatId, intervalMinutes)
                isSending = true
                viewModel.sendHeartbeatTest(context, trimmedToken, effectiveChat) { success, error ->
                    isSending = false
                    lastSentLabel = HeartbeatManager.formatLastSent(context)
                    if (success) {
                        HapticFeedbackHelper.performHaptic(context, view, HapticType.SUCCESS)
                        Toast.makeText(context, context.getString(R.string.dev_test_success), Toast.LENGTH_LONG).show()
                    } else {
                        HapticFeedbackHelper.performHaptic(context, view, HapticType.ERROR)
                        Toast.makeText(context, context.getString(R.string.dev_test_failed, error ?: ""), Toast.LENGTH_LONG).show()
                    }
                }
            },
            enabled = !isSending,
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(testInteraction, scaleDown = 0.96f),
            interactionSource = testInteraction
        ) {
            if (isSending) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.dev_sending))
            } else {
                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.dev_send_test))
            }
        }

        Text(
            text = stringResource(R.string.dev_last_sent, lastSentLabel),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
