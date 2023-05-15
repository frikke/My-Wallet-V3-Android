package com.blockchain.componentlib.system

import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.theme.Grey100

@Composable
fun LinearProgressBar(
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    val color = Blue600
    val backgroundColor = Grey100

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
