package com.blockchain.componentlib.basic

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
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
    isMultiline: Boolean = true,
    onAnnotationClicked: ((tag: String, value: String) -> Unit)? = null,
) {
    val composeColor = color.toComposeColor()
    val composeStyle = style.toComposeTypography()
    val composeTextAlign = gravity.toTextAlignment()
    if (onAnnotationClicked != null) {
        val textColor = composeColor.takeOrElse {
            composeStyle.color.takeOrElse {
                LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
            }
        }
        val mergedStyle = composeStyle.merge(
            TextStyle(
                color = textColor,
                textAlign = composeTextAlign,
            )
        )

        ClickableText(
            modifier = modifier,
            text = text,
            style = mergedStyle,
            maxLines = if (isMultiline) {
                Integer.MAX_VALUE
            } else {
                1
            },
            onClick = { offset ->
                val result = text.getStringAnnotations(offset, offset).firstOrNull() ?: return@ClickableText
                onAnnotationClicked(result.tag, result.item)
            }
        )
    } else {
        Text(
            modifier = modifier,
            text = text,
            style = composeStyle,
            color = composeColor,
            textAlign = composeTextAlign,
            maxLines = if (isMultiline) {
                Integer.MAX_VALUE
            } else {
                1
            }
        )
    }
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
