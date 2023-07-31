package com.blockchain.componentlib.system

import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun LinearProgressBar(
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    val color = AppColors.primary
    val backgroundColor = AppColors.medium

    if (progress != null) {
        LinearProgressIndicator(
            modifier = modifier,
            color = color,
            backgroundColor = backgroundColor,
            progress = progress
        )
    } else {
        LinearProgressIndicator(
            modifier = modifier,
            color = color,
            backgroundColor = backgroundColor
        )
    }
}

@Preview
@Composable
fun HorizontalProgressPreview() {
    AppTheme {
        AppSurface {
            LinearProgressBar(
                progress = 0.5f
            )
        }
    }
}
