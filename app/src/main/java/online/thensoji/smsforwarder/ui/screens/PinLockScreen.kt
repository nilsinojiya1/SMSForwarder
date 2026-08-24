package online.thensoji.smsforwarder.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import online.thensoji.smsforwarder.ui.components.PinDotsIndicator
import online.thensoji.smsforwarder.ui.components.PinKeypad
import online.thensoji.smsforwarder.util.PinManager

enum class PinMode {
    SETUP,
    UNLOCK,
    CHANGE
}

private enum class SetupStep {
    ENTER_NEW_PIN,
    CONFIRM_NEW_PIN
}

private enum class ChangeStep {
    ENTER_OLD_PIN,
    ENTER_NEW_PIN,
    CONFIRM_NEW_PIN
}

@Composable
fun PinLockScreen(
    mode: PinMode,
    onPinSuccess: () -> Unit,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var enteredPin by remember { mutableStateOf("") }
    var firstEnteredPin by remember { mutableStateOf("") }
    var setupStep by remember { mutableStateOf(SetupStep.ENTER_NEW_PIN) }
    var changeStep by remember { mutableStateOf(ChangeStep.ENTER_OLD_PIN) }

    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val title: String
    val subtitle: String

    when (mode) {
        PinMode.UNLOCK -> {
            title = "Enter PIN"
            subtitle = "Enter your 4-digit PIN to access SMS Forwarder"
        }
        PinMode.SETUP -> {
            when (setupStep) {
                SetupStep.ENTER_NEW_PIN -> {
                    title = "Set Up 4-Digit PIN"
                    subtitle = "Choose a 4-digit PIN to secure your app"
                }
                SetupStep.CONFIRM_NEW_PIN -> {
                    title = "Confirm Your PIN"
                    subtitle = "Re-enter the 4-digit PIN to confirm"
                }
            }
        }
        PinMode.CHANGE -> {
            when (changeStep) {
                ChangeStep.ENTER_OLD_PIN -> {
                    title = "Enter Current PIN"
                    subtitle = "Verify your current 4-digit PIN"
                }
                ChangeStep.ENTER_NEW_PIN -> {
                    title = "Enter New PIN"
                    subtitle = "Choose your new 4-digit PIN"
                }
                ChangeStep.CONFIRM_NEW_PIN -> {
                    title = "Confirm New PIN"
                    subtitle = "Re-enter the new 4-digit PIN to confirm"
                }
            }
        }
    }

    fun handlePinComplete(pin: String) {
        when (mode) {
            PinMode.UNLOCK -> {
                if (PinManager.verifyPin(context, pin)) {
                    isError = false
                    errorMessage = null
                    onPinSuccess()
                } else {
                    isError = true
                    errorMessage = "Incorrect PIN. Please try again."
                    coroutineScope.launch {
                        delay(600)
                        enteredPin = ""
                        isError = false
                    }
                }
            }

            PinMode.SETUP -> {
                when (setupStep) {
                    SetupStep.ENTER_NEW_PIN -> {
                        firstEnteredPin = pin
                        enteredPin = ""
                        setupStep = SetupStep.CONFIRM_NEW_PIN
                        errorMessage = null
                    }
                    SetupStep.CONFIRM_NEW_PIN -> {
                        if (pin == firstEnteredPin) {
                            PinManager.savePin(context, pin)
                            Toast.makeText(context, "✅ PIN set successfully!", Toast.LENGTH_SHORT).show()
                            onPinSuccess()
                        } else {
                            isError = true
                            errorMessage = "PINs did not match. Please try again."
                            coroutineScope.launch {
                                delay(800)
                                enteredPin = ""
                                firstEnteredPin = ""
                                setupStep = SetupStep.ENTER_NEW_PIN
                                isError = false
                            }
                        }
                    }
                }
            }

            PinMode.CHANGE -> {
                when (changeStep) {
                    ChangeStep.ENTER_OLD_PIN -> {
                        if (PinManager.verifyPin(context, pin)) {
                            enteredPin = ""
                            changeStep = ChangeStep.ENTER_NEW_PIN
                            errorMessage = null
                        } else {
                            isError = true
                            errorMessage = "Incorrect current PIN. Please try again."
                            coroutineScope.launch {
                                delay(600)
                                enteredPin = ""
                                isError = false
                            }
                        }
                    }
                    ChangeStep.ENTER_NEW_PIN -> {
                        firstEnteredPin = pin
                        enteredPin = ""
                        changeStep = ChangeStep.CONFIRM_NEW_PIN
                        errorMessage = null
                    }
                    ChangeStep.CONFIRM_NEW_PIN -> {
                        if (pin == firstEnteredPin) {
                            PinManager.savePin(context, pin)
                            Toast.makeText(context, "✅ PIN changed successfully!", Toast.LENGTH_SHORT).show()
                            onPinSuccess()
                        } else {
                            isError = true
                            errorMessage = "New PINs did not match. Please try again."
                            coroutineScope.launch {
                                delay(800)
                                enteredPin = ""
                                firstEnteredPin = ""
                                changeStep = ChangeStep.ENTER_NEW_PIN
                                isError = false
                            }
                        }
                    }
                }
            }
        }
    }

    fun onDigitPress(digit: String) {
        if (enteredPin.length < 4) {
            val newPin = enteredPin + digit
            enteredPin = newPin
            isError = false
            errorMessage = null
            if (newPin.length == 4) {
                handlePinComplete(newPin)
            }
        }
    }

    fun onBackspacePress() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            isError = false
            errorMessage = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (mode == PinMode.SETUP) Icons.Filled.Security else Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 4-Digit Dots Indicator
            PinDotsIndicator(
                pinLength = enteredPin.length,
                maxDigits = 4,
                isError = isError
            )

            // Error text if any
            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        // Bottom Keypad
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            PinKeypad(
                onDigitClick = { onDigitPress(it) },
                onBackspaceClick = { onBackspacePress() }
            )

            if (onCancel != null && mode == PinMode.CHANGE) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

