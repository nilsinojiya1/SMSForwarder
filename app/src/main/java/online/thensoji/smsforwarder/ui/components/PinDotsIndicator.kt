package online.thensoji.smsforwarder.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PinDotsIndicator(
    pinLength: Int,
    maxDigits: Int = 4,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(isError) {
        if (isError) {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    -20f at 50 using FastOutLinearInEasing
                    20f at 100 using FastOutLinearInEasing
                    -14f at 150 using FastOutLinearInEasing
                    14f at 200 using FastOutLinearInEasing
                    -7f at 250 using FastOutLinearInEasing
                    7f at 300 using FastOutLinearInEasing
                    0f at 400
                }
            )
        }
    }

    Row(
        modifier = modifier.offset(x = shakeOffset.value.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until maxDigits) {
            val isFilled = i < pinLength
            val dotColor by animateColorAsState(
                targetValue = when {
                    isError -> MaterialTheme.colorScheme.error
                    isFilled -> MaterialTheme.colorScheme.primary
                    else -> Color.Transparent
                },
                animationSpec = tween(200),
                label = "dotColor"
            )
            val borderColor by animateColorAsState(
                targetValue = when {
                    isError -> MaterialTheme.colorScheme.error
                    isFilled -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline
                },
                animationSpec = tween(200),
                label = "borderColor"
            )
            val size by animateDpAsState(
                targetValue = if (isFilled) 18.dp else 16.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "dotSize"
            )

            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(2.dp, borderColor, CircleShape)
            )
        }
    }
}
