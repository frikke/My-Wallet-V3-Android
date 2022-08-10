package com.blockchain.componentlib.basic

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import dev.jeziellago.compose.markdowntext.MarkdownText

@Composable
fun MarkdownContent(
    markdownText: String,
    modifier: Modifier = Modifier,
    style: ComposeTypographies,
    color: ComposeColors,
    gravity: ComposeGravities
) {
    MarkdownText(
        modifier = modifier,
        markdown = markdownText,
        style = style.toComposeTypography(),
        textAlign = gravity.toTextAlignment(),
        color = color.toComposeColor()
    )
}

@Preview
@Composable
fun MarkdownText_Basic() {
    AppTheme(darkTheme = false) {
        AppSurface {
            MarkdownContent(
                markdownText = "**Bold** _Italic_",
                style = ComposeTypographies.Body1,
                color = ComposeColors.Medium,
                gravity = ComposeGravities.Centre
            )
        }
    }
}
