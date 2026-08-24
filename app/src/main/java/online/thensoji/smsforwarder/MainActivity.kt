package online.thensoji.smsforwarder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import online.thensoji.smsforwarder.data.ForwardedMessage
import online.thensoji.smsforwarder.ui.MessageViewModel
import online.thensoji.smsforwarder.ui.theme.SMSforwarderTheme
import online.thensoji.smsforwarder.util.MessageFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SMSforwarderTheme {
                MainScreen()
            }
        }
    }
}

private fun checkAllPermissions(context: Context): Boolean {
    val receiveSms = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECEIVE_SMS
    ) == PackageManager.PERMISSION_GRANTED

    val readSms = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_SMS
    ) == PackageManager.PERMISSION_GRANTED

    val readPhoneState = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_PHONE_STATE
    ) == PackageManager.PERMISSION_GRANTED

    return receiveSms && readSms && readPhoneState
}

private fun getRequiredPermissions(): Array<String> {
    val list = mutableListOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_PHONE_STATE
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        list.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    return list.toTypedArray()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    val title = when (currentRoute) {
        "settings" -> "Telegram Settings"
        "messages" -> "All Messages"
        else -> "SMS Forwarder"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (currentRoute != "home") {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (currentRoute == "home") {
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                HomeScreen(
                    onOpenMessages = { navController.navigate("messages") },
                    onOpenSettings = { navController.navigate("settings") }
                )
            }
            composable("settings") {
                SettingsScreen()
            }
            composable("messages") {
                AllMessagesScreen()
            }
        }
    }
}

@Composable
fun HomeScreen(
    onOpenMessages: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: MessageViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(checkAllPermissions(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermissions = checkAllPermissions(context)
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
        hasPermissions = checkAllPermissions(context)
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
        Text("Overview", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // Status Card: Device Identity
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Smartphone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Device Tag: $deviceName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Identifies this device when multiple phones forward to the same chat.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Status Card: Permissions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (hasPermissions) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
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
                        text = if (hasPermissions) "SMS Permissions Granted" else "Permissions Missing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (hasPermissions)
                            "App can receive and read SMS messages."
                        else
                            "Grant SMS permissions so incoming SMS can be detected.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (!hasPermissions) {
                Button(
                    onClick = { permissionLauncher.launch(getRequiredPermissions()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Grant Permissions")
                }
            }
        }

        // Status Card: Telegram Config
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isConfigured) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isConfigured) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (isConfigured) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isConfigured) "Telegram Bot Configured" else "Telegram Not Configured",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isConfigured)
                            "Messages will be forwarded to your Telegram chat."
                        else
                            "Set up your Bot Token & Chat ID in Settings.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Statistics Card: Messages Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Messages Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryItem(title = "Total", count = totalCount)
                    SummaryItem(title = "Sent", count = sentCount, color = MaterialTheme.colorScheme.primary)
                    SummaryItem(title = "Pending", count = pendingCount, color = if (pendingCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    SummaryItem(title = "Delayed", count = delayedCount, color = if (delayedCount > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

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

@Composable
fun SummaryItem(
    title: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = MessageFormatter.formatCompactNumber(count),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

enum class MessageFilterTab(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    SENT("Sent"),
    DELAYED("Delayed")
}

@Composable
fun AllMessagesScreen(
    viewModel: MessageViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    var selectedTab by remember { mutableStateOf(MessageFilterTab.ALL) }

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

    val dateFormat = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "All Messages (${MessageFormatter.formatCompactNumber(messages.size)})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { viewModel.refreshMessages() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontally Scrollable Filter Chips (clean layout on all screen sizes)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTab == MessageFilterTab.ALL,
                onClick = { selectedTab = MessageFilterTab.ALL },
                label = {
                    Text(
                        "All (${MessageFormatter.formatCompactNumber(messages.size)})",
                        maxLines = 1
                    )
                }
            )
            FilterChip(
                selected = selectedTab == MessageFilterTab.PENDING,
                onClick = { selectedTab = MessageFilterTab.PENDING },
                label = {
                    Text(
                        "Pending (${MessageFormatter.formatCompactNumber(pendingList.size)})",
                        maxLines = 1
                    )
                }
            )
            FilterChip(
                selected = selectedTab == MessageFilterTab.SENT,
                onClick = { selectedTab = MessageFilterTab.SENT },
                label = {
                    Text(
                        "Sent (${MessageFormatter.formatCompactNumber(sentList.size)})",
                        maxLines = 1
                    )
                }
            )
            FilterChip(
                selected = selectedTab == MessageFilterTab.DELAYED,
                onClick = { selectedTab = MessageFilterTab.DELAYED },
                label = {
                    Text(
                        "Delayed (${MessageFormatter.formatCompactNumber(delayedList.size)})",
                        maxLines = 1
                    )
                }
            )
        }

        // Action banner if pending messages exist
        if (pendingList.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${MessageFormatter.formatCompactNumber(pendingList.size)} message(s) waiting for internet",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Auto-sends when connection is restored.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.resendAllPending(context)
                            Toast.makeText(context, "Retrying pending messages...", Toast.LENGTH_SHORT).show()
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Send Now", maxLines = 1)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = when (selectedTab) {
                            MessageFilterTab.PENDING -> Icons.Filled.CheckCircle
                            else -> Icons.Filled.Inbox
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when (selectedTab) {
                            MessageFilterTab.ALL -> "No messages yet"
                            MessageFilterTab.PENDING -> "No pending messages"
                            MessageFilterTab.SENT -> "No sent messages"
                            MessageFilterTab.DELAYED -> "No delayed messages"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (selectedTab) {
                            MessageFilterTab.ALL -> "Incoming SMS messages will appear here."
                            MessageFilterTab.PENDING -> "All messages have been successfully sent to Telegram."
                            MessageFilterTab.SENT -> "Messages forwarded to Telegram will appear here."
                            MessageFilterTab.DELAYED -> "Messages with > 1 min forwarding delay will appear here."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredList, key = { it.id }) { msg ->
                    MessageCard(
                        msg = msg,
                        dateFormat = dateFormat,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageCard(
    msg: ForwardedMessage,
    dateFormat: SimpleDateFormat,
    onResend: () -> Unit,
    onMarkSent: () -> Unit,
    onDelete: () -> Unit
) {
    val now = remember { System.currentTimeMillis() }
    val isDelayed = (msg.delayMillis ?: 0L) >= 60_000L
    val isPendingDelayed = !msg.isSent && (now - msg.timestamp) >= 60_000L

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Sent", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    )
                } else {
                    AssistChip(
                        onClick = {},
                        leadingIcon = {
                            Icon(Icons.Filled.HourglassEmpty, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Pending / Queued", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    )
                }

                // Delayed Badge
                if (isDelayed) {
                    val delayText = MessageFormatter.formatDelayDuration(msg.delayMillis ?: 0L)
                    AssistChip(
                        onClick = {},
                        leadingIcon = {
                            Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Delayed by $delayText", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    )
                } else if (isPendingDelayed) {
                    val pendingDelayText = MessageFormatter.formatDelayDuration(now - msg.timestamp)
                    AssistChip(
                        onClick = {},
                        leadingIcon = {
                            Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Offline for $pendingDelayText", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.errorContainer)
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

            // Body
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
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send Now", style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    OutlinedButton(
                        onClick = onResend,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resend", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: MessageViewModel = hiltViewModel()
) {
    val context = LocalContext.current
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Telegram Bot Configuration",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = deviceName,
            onValueChange = { deviceName = it },
            label = { Text("Device Name / Tag") },
            placeholder = { Text("e.g. $defaultDeviceName") },
            supportingText = { Text("Appended to messages to identify this device (Default: $defaultDeviceName)") },
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
            label = { Text("Telegram Bot Token") },
            placeholder = { Text("e.g. 123456789:ABCdefGhIJKlmNoPQRstuVWXyz") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                    Icon(
                        imageVector = if (isTokenVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (isTokenVisible) "Hide token" else "Show token"
                    )
                }
            }
        )

        OutlinedTextField(
            value = chatId,
            onValueChange = { chatId = it },
            label = { Text("Telegram Chat ID") },
            placeholder = { Text("e.g. 123456789 or -100123456789") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        Button(
            onClick = {
                sharedPreferences.edit {
                    putString("device_name", deviceName.trim())
                    putString("bot_token", botToken.trim())
                    putString("chat_id", chatId.trim())
                }
                Toast.makeText(context, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Settings")
        }

        OutlinedButton(
            onClick = {
                val token = botToken.trim()
                val chat = chatId.trim()
                if (token.isEmpty() || chat.isEmpty()) {
                    Toast.makeText(context, "Please enter both Bot Token and Chat ID first.", Toast.LENGTH_SHORT).show()
                    return@OutlinedButton
                }
                isTestingConnection = true
                val currentDeviceTag = deviceName.trim().ifEmpty { defaultDeviceName }
                viewModel.testTelegramConnection(token, chat, currentDeviceTag) { isSuccess, errorMsg ->
                    isTestingConnection = false
                    if (isSuccess) {
                        Toast.makeText(context, "✅ Connection successful! Test message sent to Telegram.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "❌ Connection failed: ${errorMsg ?: "Check Token, Chat ID and connection."}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            enabled = !isTestingConnection,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isTestingConnection) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Testing...")
            } else {
                Text("Test Telegram Connection")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("How to get credentials:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text("1. Open Telegram and search for @BotFather.", style = MaterialTheme.typography.bodySmall)
                Text("2. Create a new bot with /newbot to receive your Bot Token.", style = MaterialTheme.typography.bodySmall)
                Text("3. Send a message to @userinfobot to get your personal Chat ID.", style = MaterialTheme.typography.bodySmall)
                Text("4. Start your bot by opening its chat and pressing 'Start'.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}