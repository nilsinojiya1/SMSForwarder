package online.thensoji.smsforwarder.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import online.thensoji.smsforwarder.util.PinManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""

    val isPinConfigured = remember { PinManager.isPinSet(context) }
    val initialDestination = if (isPinConfigured) "unlock_pin" else "setup_pin"

    val isPinScreen = currentRoute == "setup_pin" || currentRoute == "unlock_pin" || currentRoute == "change_pin"

    val title = when (currentRoute) {
        "settings" -> "Telegram Settings"
        "messages" -> "All Messages"
        "change_pin" -> "Change PIN"
        else -> "SMS Forwarder"
    }

    Scaffold(
        topBar = {
            if (!isPinScreen) {
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
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = initialDestination,
            modifier = Modifier.padding(if (isPinScreen) androidx.compose.foundation.layout.PaddingValues() else paddingValues)
        ) {
            // First-time 4-digit PIN setup screen
            composable("setup_pin") {
                PinLockScreen(
                    mode = PinMode.SETUP,
                    onPinSuccess = {
                        navController.navigate("home") {
                            popUpTo("setup_pin") { inclusive = true }
                        }
                    }
                )
            }

            // Normal startup 4-digit PIN unlock screen
            composable("unlock_pin") {
                PinLockScreen(
                    mode = PinMode.UNLOCK,
                    onPinSuccess = {
                        navController.navigate("home") {
                            popUpTo("unlock_pin") { inclusive = true }
                        }
                    }
                )
            }

            // Change PIN screen
            composable("change_pin") {
                PinLockScreen(
                    mode = PinMode.CHANGE,
                    onPinSuccess = {
                        navController.popBackStack()
                    },
                    onCancel = {
                        navController.popBackStack()
                    }
                )
            }

            // Main app screens
            composable("home") {
                HomeScreen(
                    onOpenMessages = { navController.navigate("messages") },
                    onOpenSettings = { navController.navigate("settings") }
                )
            }

            composable("settings") {
                SettingsScreen(
                    onChangePin = { navController.navigate("change_pin") }
                )
            }

            composable("messages") {
                AllMessagesScreen()
            }
        }
    }
}
