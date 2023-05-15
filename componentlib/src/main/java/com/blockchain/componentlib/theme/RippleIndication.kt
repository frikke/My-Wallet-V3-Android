package com.blockchain.componentlib.theme

import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp

@Composable
fun rememberRippleIndicator(
    bounded: Boolean = true,
    radius: Dp = AppTheme.dimensions.hugeSpacing
): Indication = rememberRipple(
    bounded = bounded,
    radius = radius
)

fun Modifier.clickableWithIndication(
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSourceFinal = interactionSource ?: remember { MutableInteractionSource() }
    val indicationFinal = indication ?: rememberRippleIndicator()

    this.clickable(
        interactionSource = interactionSourceFinal,
        indication = indicationFinal,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = onClick
    )
}
