package online.thensoji.smsforwarder

import dagger.hilt.android.AndroidEntryPoint

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import online.thensoji.smsforwarder.ui.theme.SMSforwarderTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val permissionRequestCode = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SMSforwarderTheme {
                MainScreen()
            }
        }

        if (!arePermissionsGranted()) {
            requestPermissions()
        }
    }

    private fun arePermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_STATE),
            permissionRequestCode
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMS Forwarder") },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
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
                HomeScreen(onOpenQueue = { navController.navigate("queue") })
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
fun HomeScreen(onOpenQueue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Welcome!", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onOpenQueue) {
            Text("View queued messages")
        }
    }
}

@Composable
fun QueuedMessagesScreen(viewModel: online.thensoji.smsforwarder.ui.MessageViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    val context = LocalContext.current
    val unsent by viewModel.unsent.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshUnsent() }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text("Queued Messages", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        androidx.compose.foundation.lazy.LazyColumn {
            androidx.compose.foundation.lazy.items(unsent) { msg ->
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = "From: ${msg.sender ?: "unknown"}")
                        Text(text = "Time: ${java.util.Date(msg.timestamp)}")
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = msg.body, maxLines = 6)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Button(onClick = {
                                // enqueue worker to retry sending this message
                                val input = androidx.work.Data.Builder()
                                    .putLong("messageId", msg.id)
                                    .build()
                                val work = androidx.work.OneTimeWorkRequestBuilder<SendWorker>()
                                    .setInputData(input)
                                    .build()
                                androidx.work.WorkManager.getInstance(context).enqueue(work)
                            }) {
                                Text("Retry")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                viewModel.markAsSent(msg.id, null)
                            }) {
                                Text("Mark as Sent")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE)

    var botToken by remember { mutableStateOf(sharedPreferences.getString("bot_token", "") ?: "") }
    var chatId by remember { mutableStateOf(sharedPreferences.getString("chat_id", "") ?: "") }

    var isEditingBotToken by remember { mutableStateOf(false) }
    var isEditingChatId by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CredentialRow(
            label = "Bot Token",
            value = botToken,
            isEditing = isEditingBotToken,
            onEditToggle = { isEditingBotToken = !isEditingBotToken },
            onValueChange = { botToken = it },
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(8.dp))
        CredentialRow(
            label = "Chat ID",
            value = chatId,
            isEditing = isEditingChatId,
            onEditToggle = { isEditingChatId = !isEditingChatId },
            onValueChange = { chatId = it }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            with(sharedPreferences.edit()) {
                putString("bot_token", botToken)
                putString("chat_id", chatId)
                apply()
            }
            isEditingBotToken = false
            isEditingChatId = false
        }) {
            Text("Save Credentials")
        }
    }
}

@Composable
fun CredentialRow(
    label: String,
    value: String,
    isEditing: Boolean,
    onEditToggle: () -> Unit,
    onValueChange: (String) -> Unit,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.weight(1f),
            readOnly = !isEditing,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = if (label == "Bot Token") KeyboardType.Password else KeyboardType.Text)
        )
        IconButton(onClick = onEditToggle) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit")
        }
    }
}