package online.thensoji.smsforwarder.ui.screens

import android.widget.Toast
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import online.thensoji.smsforwarder.R
import online.thensoji.smsforwarder.ui.components.PinDotsIndicator
import online.thensoji.smsforwarder.ui.components.PinKeypad
import online.thensoji.smsforwarder.ui.components.pressScale
import online.thensoji.smsforwarder.ui.util.HapticFeedbackHelper
import online.thensoji.smsforwarder.ui.util.HapticType
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
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    var enteredPin by remember { mutableStateOf("") }
    var firstEnteredPin by remember { mutableStateOf("") }
    var setupStep by remember { mutableStateOf(SetupStep.ENTER_NEW_PIN) }
    var changeStep by remember { mutableStateOf(ChangeStep.ENTER_OLD_PIN) }

    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val title = when (mode) {
        PinMode.UNLOCK -> stringResource(R.string.pin_title_unlock)
        PinMode.SETUP -> when (setupStep) {
            SetupStep.ENTER_NEW_PIN -> stringResource(R.string.pin_title_setup)
            SetupStep.CONFIRM_NEW_PIN -> stringResource(R.string.pin_title_confirm)
        }
        PinMode.CHANGE -> when (changeStep) {
            ChangeStep.ENTER_OLD_PIN -> stringResource(R.string.pin_title_change_old)
            ChangeStep.ENTER_NEW_PIN -> stringResource(R.string.pin_title_change_new)
            ChangeStep.CONFIRM_NEW_PIN -> stringResource(R.string.pin_title_change_confirm)
        }
    }

    val subtitle = when (mode) {
        PinMode.UNLOCK -> stringResource(R.string.pin_subtitle_unlock)
        PinMode.SETUP -> when (setupStep) {
            SetupStep.ENTER_NEW_PIN -> stringResource(R.string.pin_subtitle_setup)
            SetupStep.CONFIRM_NEW_PIN -> stringResource(R.string.pin_subtitle_confirm)
        }
        PinMode.CHANGE -> when (changeStep) {
            ChangeStep.ENTER_OLD_PIN -> stringResource(R.string.pin_subtitle_change_old)
            ChangeStep.ENTER_NEW_PIN -> stringResource(R.string.pin_subtitle_change_new)
            ChangeStep.CONFIRM_NEW_PIN -> stringResource(R.string.pin_subtitle_change_confirm)
        }
    }

    fun handlePinComplete(pin: String) {
        when (mode) {
            PinMode.UNLOCK -> {
                if (PinManager.verifyPin(context, pin)) {
                    isError = false
                    errorMessage = null
                    HapticFeedbackHelper.performHaptic(context, view, HapticType.SUCCESS)
                    onPinSuccess()
                } else {
                    isError = true
                    errorMessage = context.getString(R.string.pin_error_incorrect)
                    HapticFeedbackHelper.performHaptic(context, view, HapticType.ERROR)
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
                        HapticFeedbackHelper.performHaptic(context, view, HapticType.CLICK)
                    }
                    SetupStep.CONFIRM_NEW_PIN -> {
                        if (pin == firstEnteredPin) {
                            PinManager.savePin(context, pin)
                            HapticFeedbackHelper.performHaptic(context, view, HapticType.SUCCESS)
                            Toast.makeText(context, context.getString(R.string.pin_success_set), Toast.LENGTH_SHORT).show()
                            onPinSuccess()
                        } else {
                            isError = true
                            errorMessage = context.getString(R.string.pin_error_mismatch)
                            HapticFeedbackHelper.performHaptic(context, view, HapticType.ERROR)
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
                            HapticFeedbackHelper.performHaptic(context, view, HapticType.CLICK)
                        } else {
                            isError = true
                            errorMessage = context.getString(R.string.pin_error_old_incorrect)
                            HapticFeedbackHelper.performHaptic(context, view, HapticType.ERROR)
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
                        HapticFeedbackHelper.performHaptic(context, view, HapticType.CLICK)
                    }
                    ChangeStep.CONFIRM_NEW_PIN -> {
                        if (pin == firstEnteredPin) {
                            PinManager.savePin(context, pin)
                            HapticFeedbackHelper.performHaptic(context, view, HapticType.SUCCESS)
                            Toast.makeText(context, context.getString(R.string.pin_success_changed), Toast.LENGTH_SHORT).show()
                            onPinSuccess()
                        } else {
                            isError = true
                            errorMessage = context.getString(R.string.pin_error_new_mismatch)
                            HapticFeedbackHelper.performHaptic(context, view, HapticType.ERROR)
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
                val cancelInteraction = remember { MutableInteractionSource() }
                TextButton(
                    onClick = {
                        HapticFeedbackHelper.performHaptic(context, view, HapticType.CLICK)
                        onCancel()
                    },
                    modifier = Modifier.pressScale(cancelInteraction, scaleDown = 0.94f),
                    interactionSource = cancelInteraction
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
