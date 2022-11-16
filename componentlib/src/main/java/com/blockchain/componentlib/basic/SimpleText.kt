package com.blockchain.componentlib.basic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallHorizontalSpacer

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

@Composable
fun ExpandableSimpleText(
    text: String,
    modifier: Modifier = Modifier,
    style: ComposeTypographies,
    color: ComposeColors,
    gravity: ComposeGravities,
    maxLinesWhenCollapsed: Int,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    val composeColor = color.toComposeColor()
    val composeStyle = style.toComposeTypography()
    val composeTextAlign = gravity.toTextAlignment()
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

    var expanded by remember { mutableStateOf(false) }

    Row(modifier = modifier) {
        Text(
            modifier = Modifier.weight(1f),
            text = buildAnnotatedString { append(text) },
            style = mergedStyle,
            maxLines = if (expanded) Int.MAX_VALUE else maxLinesWhenCollapsed,
            overflow = overflow,
        )

        SmallHorizontalSpacer()

        val expandIcon = when (expanded) {
            true -> ImageResource.Local(R.drawable.ic_chevron_up)
            false -> ImageResource.Local(R.drawable.ic_chevron_down)
        }

        Image(
            imageResource = expandIcon,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .size(dimensionResource(R.dimen.standard_spacing))
                .clickable { expanded = !expanded }
        )
    }
}

@Composable
fun ExpandableSimpleText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: ComposeTypographies,
    color: ComposeColors,
    gravity: ComposeGravities,
    maxLinesWhenCollapsed: Int,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    onAnnotationClicked: ((tag: String, value: String) -> Unit)? = null,
) {
    val composeColor = color.toComposeColor()
    val composeStyle = style.toComposeTypography()
    val composeTextAlign = gravity.toTextAlignment()
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

    var expanded by remember { mutableStateOf(false) }

    Row(modifier = modifier) {

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
                modifier = Modifier.weight(1f),
                text = text,
                style = mergedStyle,
                maxLines = if (expanded) Int.MAX_VALUE else maxLinesWhenCollapsed,
                onClick = { offset ->
                    val result = text.getStringAnnotations(offset, offset).firstOrNull() ?: return@ClickableText
                    onAnnotationClicked(result.tag, result.item)
                }
            )
        } else {
            Text(
                modifier = Modifier.weight(1f),
                text = text,
                style = mergedStyle,
                maxLines = if (expanded) Int.MAX_VALUE else maxLinesWhenCollapsed,
                overflow = overflow,
            )
        }

        SmallHorizontalSpacer()

        val expandIcon = when (expanded) {
            true -> ImageResource.Local(R.drawable.ic_chevron_up)
            false -> ImageResource.Local(R.drawable.ic_chevron_down)
        }

        Image(
            imageResource = expandIcon,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .size(dimensionResource(R.dimen.standard_spacing))
                .clickable { expanded = !expanded }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewExpandableSimpleText() {
    AppTheme {
        AppSurface {
            ExpandableSimpleText(
                text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed euismod, nunc sit amet aliquam" +
                    " luctus, nisi nisl aliquam nisl, nec aliquam nunc nisl sit amet nisl. Sed euismod," +
                    " nunc sit amet aliquam luctus, nisi nisl aliquam nisl, nec aliquam nunc nisl sit amet nisl. " +
                    "Sed euismod, nunc sit",
                style = ComposeTypographies.Caption1,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { },
                maxLinesWhenCollapsed = 3
            )
        }
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
