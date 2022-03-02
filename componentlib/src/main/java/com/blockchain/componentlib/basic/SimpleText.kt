package com.blockchain.componentlib.basic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun SimpleText(
    text: String,
    style: ComposeTypographies,
    color: ComposeColors,
    gravity: ComposeGravities,
    onClick: (() -> Unit)? = null
) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = onClick != null,
                onClick = {
                    onClick?.let {
                        it()
                    }
                }
            ),
        text = text,
        style = style.toComposeTypography(),
        color = color.toComposeColor(),
        textAlign = gravity.toTextAlignment()
    )
}

@Preview
@Composable
fun Text_Medium_Body1_Light() {
    AppTheme(darkTheme = false) {
        AppSurface {
            SimpleText(
                text = "Sample Text",
                style = ComposeTypographies.Body1,
                color = ComposeColors.Medium,
                gravity = ComposeGravities.Centre
            )
        }
    }
}
