package online.thensoji.smsforwarder.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import online.thensoji.smsforwarder.ui.MessageViewModel
import online.thensoji.smsforwarder.ui.components.*
import online.thensoji.smsforwarder.util.MessageFormatter
import online.thensoji.smsforwarder.util.PermissionUtils

@Composable
fun HomeScreen(
    onOpenMessages: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: MessageViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(PermissionUtils.checkAllPermissions(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermissions = PermissionUtils.checkAllPermissions(context)
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

    val messages by viewModel.messages.collectAsState()

    LaunchedEffect(Unit) {
        botToken = sharedPreferences.getString("bot_token", "") ?: ""
        chatId = sharedPreferences.getString("chat_id", "") ?: ""
        deviceName = MessageFormatter.getDeviceName(context)
        hasPermissions = PermissionUtils.checkAllPermissions(context)
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
            text = "Overview",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Status Card: Device Identity
        DeviceTagCard(deviceName = deviceName)

        // Status Card: Permissions
        PermissionsCard(
            hasPermissions = hasPermissions,
            onRequestPermissions = { permissionLauncher.launch(PermissionUtils.getRequiredPermissions()) }
        )

        // Status Card: Telegram Config
        TelegramStatusCard(isConfigured = isConfigured)

        // Statistics Card: Messages Summary
        MessagesSummaryCard(
            totalCount = totalCount,
            sentCount = sentCount,
            pendingCount = pendingCount,
            delayedCount = delayedCount
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Action Buttons
        Button(
            onClick = onOpenMessages,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("View All Messages (${MessageFormatter.formatCompactNumber(totalCount)})")
        }

        OutlinedButton(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Configure Telegram Settings")
        }
    }
}

