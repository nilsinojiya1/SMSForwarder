package online.thensoji.smsforwarder.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import online.thensoji.smsforwarder.ui.util.HapticFeedbackHelper
import online.thensoji.smsforwarder.ui.util.HapticType

/**
 * Adds a responsive, springy visual scale animation when pressed down,
 * along with ripple indication and crisp haptic feedback on click.
 */
@Composable
fun Modifier.bounceClickable(
    enabled: Boolean = true,
    scaleDown: Float = 0.93f,
    hapticType: HapticType = HapticType.CLICK,
    onClick: () -> Unit
): Modifier {
    val context = LocalContext.current
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDown else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounce_scale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true),
            enabled = enabled,
            onClick = {
                HapticFeedbackHelper.performHaptic(context, view, hapticType)
                onClick()
            }
        )
}

/**
 * Adds a subtle visual press scale effect to any existing clickable component
 * (such as standard Material 3 Buttons, Cards, or IconButtons) linked to its InteractionSource.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    scaleDown: Float = 0.96f
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "press_scale"
    )

    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
