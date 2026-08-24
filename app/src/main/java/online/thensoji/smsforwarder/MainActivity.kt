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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import online.thensoji.smsforwarder.ui.MessageViewModel
import online.thensoji.smsforwarder.ui.theme.SMSforwarderTheme
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
        "queue" -> "Queued Messages"
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
                    onOpenQueue = { navController.navigate("queue") },
                    onOpenSettings = { navController.navigate("settings") }
                )
            }
            composable("settings") {
                SettingsScreen()
            }
            composable("queue") {
                QueuedMessagesScreen()
            }
        }
    }
}

@Composable
fun HomeScreen(
    onOpenQueue: () -> Unit,
    onOpenSettings: () -> Unit
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

    LaunchedEffect(Unit) {
        botToken = sharedPreferences.getString("bot_token", "") ?: ""
        chatId = sharedPreferences.getString("chat_id", "") ?: ""
        hasPermissions = checkAllPermissions(context)
    }

    val isConfigured = botToken.isNotBlank() && chatId.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Overview", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

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
                    .padding(16.dp),
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
                        .padding(horizontal = 16.dp, vertical = 8.dp)
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
                    .padding(16.dp),
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

        Spacer(modifier = Modifier.height(8.dp))

        // Action Buttons
        Button(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Configure Telegram Settings")
        }

        OutlinedButton(
            onClick = onOpenQueue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Queued Messages")
        }
    }
}

@Composable
fun QueuedMessagesScreen(
    viewModel: MessageViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val unsent by viewModel.unsent.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshUnsent()
    }

    val dateFormat = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Unsent Messages (${unsent.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { viewModel.refreshUnsent() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (unsent.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No queued messages",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "All incoming SMS messages have been forwarded.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(unsent, key = { it.id }) { msg ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "From: ${msg.sender ?: "Unknown"}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = dateFormat.format(Date(msg.timestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = msg.body, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.markAsSent(msg.id, null)
                                    }
                                ) {
                                    Text("Mark as Sent")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val input = Data.Builder()
                                            .putLong("messageId", msg.id)
                                            .build()
                                        val work = OneTimeWorkRequestBuilder<SendWorker>()
                                            .setInputData(input)
                                            .build()
                                        WorkManager.getInstance(context).enqueue(work)
                                        Toast.makeText(context, "Retrying to forward...", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Telegram Bot Configuration",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
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
                with(sharedPreferences.edit()) {
                    putString("bot_token", botToken.trim())
                    putString("chat_id", chatId.trim())
                    apply()
                }
                Toast.makeText(context, "Credentials saved successfully!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Credentials")
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
                viewModel.testTelegramConnection(token, chat) { isSuccess, errorMsg ->
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