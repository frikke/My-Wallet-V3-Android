package com.blockchain.componentlib.tag

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.ChevronRight
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Info
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue400
import com.blockchain.componentlib.theme.Dark600
import com.blockchain.componentlib.utils.conditional

@Composable
fun Tag(
    text: String,
    size: TagSize,
    backgroundColor: Color,
    textColor: Color,
    startImageResource: ImageResource.Local? = null,
    onClick: (() -> Unit)?
) {
    val paddingHorizontal = when (size) {
        TagSize.Primary -> 8.dp
        TagSize.Large -> dimensionResource(com.blockchain.componentlib.R.dimen.very_small_spacing)
    }

    val paddingVertical = when (size) {
        TagSize.Primary -> dimensionResource(com.blockchain.componentlib.R.dimen.smallest_spacing)
        TagSize.Large -> dimensionResource(com.blockchain.componentlib.R.dimen.minuscule_spacing)
    }

    val textStyle = when (size) {
        TagSize.Primary -> AppTheme.typography.caption2
        TagSize.Large -> AppTheme.typography.paragraph2
    }

    Surface(
        color = backgroundColor,
        shape = AppTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .conditional(onClick != null) {
                    clickable { onClick!!.invoke() }
                }
                .padding(horizontal = paddingHorizontal, vertical = paddingVertical),
            verticalAlignment = Alignment.CenterVertically
        ) {
            (Icons.Info.takeIf { onClick != null } ?: startImageResource)?.let { startImg ->
                Image(
                    imageResource = startImg
                        .withSize(AppTheme.dimensions.smallSpacing)
                        .withTint(textColor)
                )
                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
            }

            Text(
                modifier = Modifier.weight(1F, fill = false),
                text = text,
                style = textStyle,
                color = textColor
            )

            onClick?.let {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))
                Image(
                    imageResource = Icons.ChevronRight
                        .withSize(AppTheme.dimensions.smallSpacing)
                        .withTint(textColor)
                )
            }
        }
    }
}

@Preview
@Composable
fun ClickableTagLargeText() {
    Tag(
        text = "There is a notice up on our status page. Full wallet functionality might not be available. Rest assured that your funds are safe. Learn more",
        size = TagSize.Primary,
        backgroundColor = Dark600,
        textColor = Blue400,
        onClick = { }
    )
}

@Preview
@Composable
fun ClickableTag() {
    Tag(
        text = "Clickable",
        size = TagSize.Primary,
        backgroundColor = Dark600,
        textColor = Blue400,
        onClick = { }
    )
}

@Preview
@Composable
fun NonClickableTag() {
    Tag(
        text = "Dummy",
        size = TagSize.Primary,
        backgroundColor = Dark600,
        textColor = Blue400,
        onClick = null
    )
}
