package com.blockchain.componentlib.system

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey600

@Composable
fun CircularProgressBar(
    modifier: Modifier = Modifier,
    text: String? = null,
    progress: Float? = null
) {
    val color = Blue600
    val backgroundColor = Grey000
    val fontStyle = AppTheme.typography.body1

    val textHeight = with(LocalDensity.current) {
        fontStyle.lineHeight.toDp()
    }

    var boxModifier = modifier

    if (text != null) {
        boxModifier = modifier.size(textHeight)
    }

    Row {
        Box(modifier = boxModifier) {
            CircularProgressIndicator(
                color = backgroundColor,
                progress = 1f
            )
            if (progress != null) {
                CircularProgressIndicator(
                    color = color,
                    progress = progress
                )
            } else {
                CircularProgressIndicator(
                    color = color
                )
            }
        }

        if (text != null) {
            Text(
                modifier = Modifier
                    .padding(start = 25.dp)
                    .align(Alignment.Bottom),
                text = text,
                style = fontStyle,
                color = Grey600
            )
        }
    }
}

@Preview
@Composable
fun CircularProgressBarPreview() {
    AppTheme {
        AppSurface {
            CircularProgressBar(
                progress = 0.5f,
                text = "Checking for Update..."
            )
        }
    }
}

@Preview
@Composable
fun CircularProgressBarPreviewNoText() {
    AppTheme {
        AppSurface {
            CircularProgressBar()
        }
    }
}
