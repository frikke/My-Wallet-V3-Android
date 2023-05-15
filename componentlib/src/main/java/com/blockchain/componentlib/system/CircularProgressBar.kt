package com.blockchain.componentlib.system

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey600

@Composable
fun CircularProgressBar(
    modifier: Modifier = Modifier,
    text: String? = null,
    progress: Float? = null
) {
    val color = AppTheme.colors.primary
    val backgroundColor = Grey000
    val fontStyle = AppTheme.typography.body1

    val textHeight = with(LocalDensity.current) {
        fontStyle.lineHeight.toDp()
    }

    val boxModifier = if (text != null) {
        Modifier.size(textHeight)
    } else {
        Modifier
    }

    Row(modifier = modifier, horizontalArrangement = Arrangement.Center) {
        Box(boxModifier) {
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
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontFeatureSettings = "tnum")) {
                        append(text)
                    }
                },
                style = fontStyle,
                color = Grey600
            )
        }
    }
}

@Composable
fun CircularProgressBarWithSmallText(
    modifier: Modifier = Modifier,
    text: String? = null,
    progress: Float? = null
) {
    val color = AppTheme.colors.primary
    val backgroundColor = Grey000
    val fontStyle = AppTheme.typography.micro1

    val textHeight = with(LocalDensity.current) {
        fontStyle.lineHeight.toDp()
    }

    val boxModifier = if (text != null) {
        Modifier.size(textHeight)
    } else {
        Modifier
    }

    Row(modifier = modifier, horizontalArrangement = Arrangement.Center) {
        Box(boxModifier) {
            CircularProgressIndicator(
                color = backgroundColor,
                progress = 1f,
                strokeWidth = 2.dp
            )
            if (progress != null) {
                CircularProgressIndicator(
                    color = color,
                    progress = progress,
                    strokeWidth = 2.dp
                )
            } else {
                CircularProgressIndicator(
                    color = color,
                    strokeWidth = 2.dp
                )
            }
        }

        if (text != null) {
            Text(
                modifier = Modifier
                    .padding(start = 25.dp)
                    .align(Alignment.Bottom),
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontFeatureSettings = "tnum")) {
                        append(text)
                    }
                },
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
                progress = 0.7f,
                text = "Checking for Update..."
            )
        }
    }
}

@Preview
@Composable
fun CircularProgressBarNumberPreview() {
    AppTheme {
        AppSurface {
            CircularProgressBar(
                progress = 0.5f,
                text = "15:00 mins remaining"
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
