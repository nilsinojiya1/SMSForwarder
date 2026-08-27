package online.thensoji.smsforwarder.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import online.thensoji.smsforwarder.R
import online.thensoji.smsforwarder.ui.components.SecurityConsentDialog
import online.thensoji.smsforwarder.ui.components.pressScale
import online.thensoji.smsforwarder.ui.util.HapticFeedbackHelper
import online.thensoji.smsforwarder.ui.util.HapticType
import online.thensoji.smsforwarder.util.ConsentManager
import online.thensoji.smsforwarder.util.PinManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""

    var isConsentGiven by remember {
        mutableStateOf(ConsentManager.isConsentGiven(context))
    }
    var showConsentReviewDialog by remember { mutableStateOf(false) }

    // Mandatory first-time prominent disclosure & ethical consent dialog
    if (!isConsentGiven) {
        SecurityConsentDialog(
            isReviewMode = false,
            onAccept = {
                HapticFeedbackHelper.performHaptic(context, view, HapticType.SUCCESS)
                ConsentManager.setConsentGiven(context, true)
                isConsentGiven = true
            },
            onDecline = {
                (context as? Activity)?.finishAffinity()
            }
        )
    }

    // On-demand review mode from settings
    if (showConsentReviewDialog) {
        SecurityConsentDialog(
            isReviewMode = true,
            onDismiss = {
                HapticFeedbackHelper.performHaptic(context, view, HapticType.CLICK)
                showConsentReviewDialog = false
            }
        )
    }

    val isPinConfigured = remember { PinManager.isPinSet(context) }
    val initialDestination = if (isPinConfigured) "unlock_pin" else "setup_pin"

    val isPinScreen = currentRoute == "setup_pin" || currentRoute == "unlock_pin" || currentRoute == "change_pin"

    val title = when (currentRoute) {
        "settings" -> stringResource(R.string.nav_settings)
        "messages" -> stringResource(R.string.nav_messages)
        "change_pin" -> stringResource(R.string.nav_change_pin)
        else -> stringResource(R.string.app_name)
    }

    Scaffold(
        topBar = {
            if (!isPinScreen) {
                TopAppBar(
                    title = {
                        AnimatedContent(
                            targetState = title,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)) togetherWith
                                        fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
                            },
                            label = "top_bar_title_anim"
                        ) { targetTitle ->
                            Text(
                                text = targetTitle,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    navigationIcon = {
                        if (currentRoute != "home") {
                            val backInteractionSource = remember { MutableInteractionSource() }
                            IconButton(
                                onClick = {
                                    HapticFeedbackHelper.performHaptic(context, view, HapticType.CLICK)
                                    navController.popBackStack()
                                },
                                modifier = Modifier.pressScale(backInteractionSource, scaleDown = 0.90f),
                                interactionSource = backInteractionSource
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back)
                                )
                            }
                        }
                    },
                    actions = {
                        if (currentRoute == "home") {
                            val settingsInteractionSource = remember { MutableInteractionSource() }
                            IconButton(
                                onClick = {
                                    HapticFeedbackHelper.performHaptic(context, view, HapticType.CLICK)
                                    navController.navigate("settings")
                                },
                                modifier = Modifier.pressScale(settingsInteractionSource, scaleDown = 0.90f),
                                interactionSource = settingsInteractionSource
                            ) {
                                Icon(
                                    Icons.Filled.Settings,
                                    contentDescription = stringResource(R.string.action_settings)
                                )
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
            modifier = Modifier.padding(if (isPinScreen) PaddingValues() else paddingValues),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    targetOffset = { it / 4 },
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(250))
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    initialOffset = { -it / 4 },
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(250))
            }
        ) {
            // First-time 4-digit PIN setup screen
            composable(
                route = "setup_pin",
                exitTransition = {
                    fadeOut(animationSpec = tween(300))
                }
            ) {
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
            composable(
                route = "unlock_pin",
                exitTransition = {
                    fadeOut(animationSpec = tween(300))
                }
            ) {
                PinLockScreen(
                    mode = PinMode.UNLOCK,
                    onPinSuccess = {
                        navController.navigate("home") {
                            popUpTo("unlock_pin") { inclusive = true }
                        }
                    }
                )
            }

            // Change PIN modal sub-screen
            composable(
                route = "change_pin",
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(300))
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(250))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(300))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(250))
                }
            ) {
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
            composable(
                route = "home",
                enterTransition = {
                    if (initialState.destination.route == "unlock_pin" || initialState.destination.route == "setup_pin") {
                        fadeIn(animationSpec = tween(380)) + scaleIn(
                            initialScale = 0.95f,
                            animationSpec = tween(380, easing = FastOutSlowInEasing)
                        )
                    } else {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.End,
                            initialOffset = { -it / 4 },
                            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(300))
                    }
                }
            ) {
                HomeScreen(
                    onOpenMessages = { navController.navigate("messages") },
                    onOpenSettings = { navController.navigate("settings") }
                )
            }

            composable("settings") {
                SettingsScreen(
                    onChangePin = { navController.navigate("change_pin") },
                    onReviewConsent = { showConsentReviewDialog = true }
                )
            }

            composable("messages") {
                AllMessagesScreen()
            }
        }
    }
}
