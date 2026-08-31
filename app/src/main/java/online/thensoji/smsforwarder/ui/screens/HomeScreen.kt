package online.thensoji.smsforwarder.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import online.thensoji.smsforwarder.R
import online.thensoji.smsforwarder.ui.MessageViewModel
import online.thensoji.smsforwarder.ui.components.*
import online.thensoji.smsforwarder.ui.util.HapticFeedbackHelper
import online.thensoji.smsforwarder.ui.util.HapticType
import online.thensoji.smsforwarder.util.HeartbeatManager
import online.thensoji.smsforwarder.util.MessageFormatter
import online.thensoji.smsforwarder.util.PermissionUtils

@Composable
fun HomeScreen(
    onOpenMessages: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: MessageViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    var hasPermissions by remember { mutableStateOf(PermissionUtils.checkAllPermissions(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermissions = PermissionUtils.checkAllPermissions(context)
    }

    // Dedicated notification-permission request (Android 13+). Requested once on launch so the
    // persistent keep-alive notification can be shown even for users who already granted SMS perms.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val sharedPreferences = remember {
        context.getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE)
    }

    var botToken by remember {
        mutableStateOf(sharedPreferences.getString("bot_token", "") ?: "")
    }
    var chatId by remember {
        mutableStateOf(sharedPreferences.getString("chat_id", "") ?: "")
    }
    var deviceName by remember {
        mutableStateOf(MessageFormatter.getDeviceName(context))
    }

    var heartbeatEnabled by remember { mutableStateOf(HeartbeatManager.isEnabled(context)) }
    var heartbeatLastSent by remember { mutableStateOf(HeartbeatManager.getLastSent(context)) }
    var heartbeatInterval by remember { mutableStateOf(HeartbeatManager.getIntervalMinutes(context)) }
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    var batteryUnrestricted by remember {
        mutableStateOf(powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false)
    }

    val messages by viewModel.messages.collectAsState()

    LaunchedEffect(Unit) {
        botToken = sharedPreferences.getString("bot_token", "") ?: ""
        chatId = sharedPreferences.getString("chat_id", "") ?: ""
        deviceName = MessageFormatter.getDeviceName(context)
        hasPermissions = PermissionUtils.checkAllPermissions(context)
        heartbeatEnabled = HeartbeatManager.isEnabled(context)
        heartbeatLastSent = HeartbeatManager.getLastSent(context)
        heartbeatInterval = HeartbeatManager.getIntervalMinutes(context)
        batteryUnrestricted = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        viewModel.refreshMessages()
    }

    val isConfigured = botToken.isNotBlank() && chatId.isNotBlank()
    val totalCount = messages.size
    val pendingCount = messages.count { !it.isSent }
    val sentCount = messages.count { it.isSent }
    val delayedCount = messages.count { (it.delayMillis ?: 0L) >= 60_000L }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = stringResource(R.string.overview_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Status Card: Device Identity
        DeviceTagCard(deviceName = deviceName)

        // Status Card: Permissions
        PermissionsCard(
            hasPermissions = hasPermissions,
            onRequestPermissions = {
                HapticFeedbackHelper.performHaptic(context, view, HapticType.CLICK)
                permissionLauncher.launch(PermissionUtils.getRequiredPermissions())
            }
        )

        // Status Card: Telegram Config (Clickable -> Settings)
        TelegramStatusCard(
            isConfigured = isConfigured,
            onClick = onOpenSettings
        )

        // Statistics Card: Messages Summary (Clickable -> All Messages)
        MessagesSummaryCard(
            totalCount = totalCount,
            sentCount = sentCount,
            pendingCount = pendingCount,
            delayedCount = delayedCount,
            onClick = onOpenMessages
        )

        // Background-health card (only visible once heartbeat monitoring is enabled)
        if (heartbeatEnabled) {
            HeartbeatHealthCard(
                lastSentMillis = heartbeatLastSent,
                intervalMinutes = heartbeatInterval,
                batteryUnrestricted = batteryUnrestricted
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Action Buttons with Spring Press Feedback
        val messagesInteraction = remember { MutableInteractionSource() }
        Button(
            onClick = {
                HapticFeedbackHelper.performHaptic(context, view, HapticType.CLICK)
                onOpenMessages()
            },
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(messagesInteraction, scaleDown = 0.96f),
            interactionSource = messagesInteraction
        ) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.action_view_all_messages, MessageFormatter.formatCompactNumber(totalCount)))
        }

        val settingsInteraction = remember { MutableInteractionSource() }
        OutlinedButton(
            onClick = {
                HapticFeedbackHelper.performHaptic(context, view, HapticType.CLICK)
                onOpenSettings()
            },
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(settingsInteraction, scaleDown = 0.96f),
            interactionSource = settingsInteraction
        ) {
            Icon(Icons.Filled.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.action_configure_telegram))
        }
    }
}
