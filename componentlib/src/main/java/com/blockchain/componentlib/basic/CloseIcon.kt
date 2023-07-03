package com.blockchain.componentlib.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.blockchain.componentlib.icons.Close
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.clickableNoEffect

@Composable
fun CloseIcon(
    modifier: Modifier = Modifier,
    isScreenBackgroundSecondary: Boolean = true,
    onClick: () -> Unit
) {
    Box(modifier = modifier) {
        Image(
            modifier = Modifier.clickableNoEffect { onClick() },
            imageResource = Icons.Close.withTint(AppColors.muted)
                .withBackground(
                    backgroundColor = if (isScreenBackgroundSecondary) {
                        // screen bg is secondary - use light bg
                        AppColors.light
                    } else {
                        // screen bg is background - use bg secondary
                        AppColors.backgroundSecondary
                    },
                    backgroundSize = AppTheme.dimensions.standardSpacing
                )
        )
    }
}
