package online.thensoji.smsforwarder.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.core.content.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import online.thensoji.smsforwarder.ui.MessageViewModel
import online.thensoji.smsforwarder.ui.components.TelegramGuideCard
import online.thensoji.smsforwarder.util.MessageFormatter
import online.thensoji.smsforwarder.util.PinManager

@Composable
fun SettingsScreen(
    onChangePin: (() -> Unit)? = null,
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
            text = "Telegram Bot Configuration",
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

        // Security / Change PIN Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "App Security",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "4-digit PIN is active to secure your app access.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (onChangePin != null) {
                    OutlinedButton(
                        onClick = onChangePin,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change 4-Digit PIN")
                    }
                }
            }
        }

        // Credentials Help Guide
        TelegramGuideCard()
    }
}
