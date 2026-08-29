package online.thensoji.smsforwarder.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.Smartphone
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
import androidx.core.content.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import online.thensoji.smsforwarder.BuildConfig
import online.thensoji.smsforwarder.R
import online.thensoji.smsforwarder.ui.MessageViewModel
import online.thensoji.smsforwarder.ui.components.TelegramGuideCard
import online.thensoji.smsforwarder.ui.components.pressScale
import online.thensoji.smsforwarder.ui.util.HapticFeedbackHelper
import online.thensoji.smsforwarder.ui.util.HapticType
import online.thensoji.smsforwarder.util.MessageFormatter

@Composable
fun SettingsScreen(
    onChangePin: (() -> Unit)? = null,
    onReviewConsent: (() -> Unit)? = null,
    viewModel: MessageViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val sharedPreferences = remember {
        context.getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE)
    }

    val defaultDeviceName = remember { MessageFormatter.getDefaultDeviceName() }
    var deviceName by remember {
        mutableStateOf(sharedPreferences.getString("device_name", "") ?: "")
    }
    var botToken by remember {
        mutableStateOf(sharedPreferences.getString("bot_token", "") ?: "")
    }
    var chatId by remember {
        mutableStateOf(sharedPreferences.getString("chat_id", "") ?: "")
    }
    var isTokenVisible by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }

    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isIgnoringBatteryOptimizations = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_bot_config_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = deviceName,
            onValueChange = { deviceName = it },
            label = { Text(stringResource(R.string.settings_device_name_label)) },
            placeholder = { Text(stringResource(R.string.settings_device_name_placeholder, defaultDeviceName)) },
            supportingText = { Text(stringResource(R.string.settings_device_name_support, defaultDeviceName)) },
            leadingIcon = {
                Icon(Icons.Filled.Smartphone, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        OutlinedTextField(
            value = botToken,
            onValueChange = { botToken = it },
            label = { Text(stringResource(R.string.settings_bot_token_label)) },
            placeholder = { Text(stringResource(R.string.settings_bot_token_placeholder)) },
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
                        contentDescription = if (isTokenVisible)
                            stringResource(R.string.settings_hide_token)
                        else
                            stringResource(R.string.settings_show_token)
                    )
                }
            }
        )

        OutlinedTextField(
            value = chatId,
            onValueChange = { chatId = it },
            label = { Text(stringResource(R.string.settings_chat_id_label)) },
            placeholder = { Text(stringResource(R.string.settings_chat_id_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        val saveInteraction = remember { MutableInteractionSource() }
        Button(
            onClick = {
                keyboardController?.hide()
                focusManager.clearFocus()
                HapticFeedbackHelper.performHaptic(context, view, HapticType.SUCCESS)
                sharedPreferences.edit {
                    putString("device_name", deviceName.trim())
                    putString("bot_token", botToken.trim())
                    putString("chat_id", chatId.trim())
                }
                Toast.makeText(context, context.getString(R.string.settings_save_success), Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(saveInteraction, scaleDown = 0.96f),
            interactionSource = saveInteraction
        ) {
            Text(stringResource(R.string.settings_save_button), fontWeight = FontWeight.SemiBold)
        }

        val testInteraction = remember { MutableInteractionSource() }
        OutlinedButton(
            onClick = {
                keyboardController?.hide()
                focusManager.clearFocus()
                HapticFeedbackHelper.performHaptic(context, view, HapticType.CLICK)
                val token = botToken.trim()
                val chat = chatId.trim()
                if (token.isEmpty() || chat.isEmpty()) {
                    HapticFeedbackHelper.performHaptic(context, view, HapticType.ERROR)
                    Toast.makeText(context, context.getString(R.string.settings_test_empty_error), Toast.LENGTH_SHORT).show()
                    return@OutlinedButton
                }
                isTestingConnection = true
                val currentDeviceTag = deviceName.trim().ifEmpty { defaultDeviceName }
                viewModel.testTelegramConnection(token, chat, currentDeviceTag) { isSuccess, errorMsg ->
                    isTestingConnection = false
                    if (isSuccess) {
                        HapticFeedbackHelper.performHaptic(context, view, HapticType.SUCCESS)
                        Toast.makeText(context, context.getString(R.string.settings_test_success), Toast.LENGTH_LONG).show()
                    } else {
                        HapticFeedbackHelper.performHaptic(context, view, HapticType.ERROR)
                        val fallbackDesc = context.getString(R.string.settings_test_check_fallback)
                        Toast.makeText(context, context.getString(R.string.settings_test_failed, errorMsg ?: fallbackDesc), Toast.LENGTH_LONG).show()
                    }
                }
            },
            enabled = !isTestingConnection,
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(testInteraction, scaleDown = 0.96f),
            interactionSource = testInteraction
        ) {
            if (isTestingConnection) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.settings_testing_button))
            } else {
                Text(stringResource(R.string.settings_test_button))
            }
        }

        // Security / Change PIN Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = stringResource(R.string.settings_security_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_security_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (onChangePin != null) {
                    val changePinInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = {
                            HapticFeedbackHelper.performHaptic(context, view, HapticType.CLICK)
                            onChangePin()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale(changePinInteraction, scaleDown = 0.96f),
                        interactionSource = changePinInteraction
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_change_pin_button))
                    }
                }
                if (onReviewConsent != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val consentInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = {
                            HapticFeedbackHelper.performHaptic(context, view, HapticType.CLICK)
                            onReviewConsent()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale(consentInteraction, scaleDown = 0.96f),
                        interactionSource = consentInteraction
                    ) {
                        Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_view_consent_button))
                    }
                }
            }
        }

        // Background Battery Optimization Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = stringResource(R.string.settings_battery_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_battery_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (isIgnoringBatteryOptimizations) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.settings_battery_unrestricted),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    val batteryInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = {
                            HapticFeedbackHelper.performHaptic(context, view, HapticType.CLICK)
                            val reqIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(reqIntent)
                            } catch (_: Exception) {
                                val altIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(altIntent)
                                } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale(batteryInteraction, scaleDown = 0.96f),
                        interactionSource = batteryInteraction
                    ) {
                        Icon(Icons.Filled.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_battery_button))
                    }
                }
            }
        }

        // Updates & Google Play Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = stringResource(R.string.settings_updates_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_updates_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                val updateInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        HapticFeedbackHelper.performHaptic(context, view, HapticType.CLICK)
                        val playStoreUrl = "https://play.google.com/store/apps/details?id=online.thensoji.smsforwarder"
                        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=online.thensoji.smsforwarder")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(marketIntent)
                        } catch (_: Exception) {
                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(webIntent)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScale(updateInteraction, scaleDown = 0.96f),
                    interactionSource = updateInteraction
                ) {
                    Icon(Icons.Filled.Shop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_check_updates_button))
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }

        // Credentials Help Guide
        TelegramGuideCard()

        // App Version & Build Code Footer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "v${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
