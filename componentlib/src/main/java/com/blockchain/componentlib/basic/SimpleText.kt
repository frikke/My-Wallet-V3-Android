package com.blockchain.componentlib.basic

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun SimpleText(
    text: String,
    modifier: Modifier = Modifier,
    style: ComposeTypographies,
    color: ComposeColors,
    gravity: ComposeGravities,
    isMultiline: Boolean = true
) {
    Text(
        modifier = modifier,
        text = text,
        style = style.toComposeTypography(),
        color = color.toComposeColor(),
        textAlign = gravity.toTextAlignment(),
        maxLines = if (isMultiline) {
            Integer.MAX_VALUE
        } else {
            1
        }
    )
}

@Composable
fun SimpleText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: ComposeTypographies,
    color: ComposeColors,
    gravity: ComposeGravities,
    isMultiline: Boolean = true
) {
    Text(
        modifier = modifier,
        text = text,
        style = style.toComposeTypography(),
        color = color.toComposeColor(),
        textAlign = gravity.toTextAlignment(),
        maxLines = if (isMultiline) {
            Integer.MAX_VALUE
        } else {
            1
        }
    )
}

@Preview
@Composable
fun Text_Medium_Body1_Light() {
    AppTheme(darkTheme = false) {
        AppSurface {
            SimpleText(
                modifier = Modifier.fillMaxWidth(),
                text = "Sample Text",
                style = ComposeTypographies.Body1,
                color = ComposeColors.Medium,
                gravity = ComposeGravities.Centre
            )
        }
    }
}
