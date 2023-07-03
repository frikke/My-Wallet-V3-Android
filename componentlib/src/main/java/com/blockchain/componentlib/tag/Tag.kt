package com.blockchain.componentlib.tag

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.ChevronRight
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Info
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue400
import com.blockchain.componentlib.theme.Dark600

@Composable
fun Tag(
    text: String,
    size: TagSize,
    defaultBackgroundColor: Color,
    defaultTextColor: Color,
    borders: Boolean = false,
    startImageResource: ImageResource = Icons.Info.withTint(defaultTextColor),
    endImageResource: ImageResource = Icons.ChevronRight.withTint(defaultTextColor),
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

    onClick?.let { action ->
        Row(
            modifier = Modifier
                .border(
                    width = if (borders) 1.dp else 0.dp,
                    color = AppTheme.colors.light,
                    shape = RoundedCornerShape(
                        size = dimensionResource(com.blockchain.componentlib.R.dimen.smallest_spacing)
                    )
                )
                .clip(
                    RoundedCornerShape(size = dimensionResource(com.blockchain.componentlib.R.dimen.smallest_spacing))
                )
                .background(defaultBackgroundColor)
                .padding(horizontal = paddingHorizontal, vertical = paddingVertical)
                .clickable(onClick = action),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (startImageResource != ImageResource.None) {
                Image(imageResource = startImageResource)
                Spacer(
                    modifier = Modifier.width(
                        width = dimensionResource(com.blockchain.componentlib.R.dimen.minuscule_spacing)
                    )
                )
            }

            Text(
                modifier = Modifier.weight(1F, fill = false),
                text = text,
                style = textStyle,
                color = defaultTextColor
            )

            if (endImageResource != ImageResource.None) {
                Spacer(
                    modifier = Modifier.width(
                        width = dimensionResource(com.blockchain.componentlib.R.dimen.tiny_spacing)
                    )
                )
                Image(imageResource = endImageResource)
            }
        }
    } ?: run {
        Text(
            text = text,
            style = textStyle,
            color = defaultTextColor,
            modifier = Modifier
                .border(
                    width = if (borders) 1.dp else 0.dp,
                    color = AppTheme.colors.light,
                    shape = RoundedCornerShape(
                        size = dimensionResource(com.blockchain.componentlib.R.dimen.smallest_spacing)
                    )
                )
                .clip(
                    RoundedCornerShape(size = dimensionResource(com.blockchain.componentlib.R.dimen.smallest_spacing))
                )
                .background(defaultBackgroundColor)
                .padding(horizontal = paddingHorizontal, vertical = paddingVertical)
        )
    }
}

@Preview
@Composable
fun ClickableTagLargeText() {
    Tag(
        text = "There is a notice up on our status page. Full wallet functionality might not be available. Rest assured that your funds are safe. Learn more",
        size = TagSize.Primary,
        defaultBackgroundColor = Dark600,
        defaultTextColor = Blue400,
        borders = true,
        onClick = { }
    )
}

@Preview
@Composable
fun ClickableTag() {
    Tag(
        text = "Clickable",
        size = TagSize.Primary,
        defaultBackgroundColor = Dark600,
        defaultTextColor = Blue400,
        borders = true,
        onClick = { }
    )
}

@Preview
@Composable
fun NonClickableTag() {
    Tag(
        text = "Dummy",
        size = TagSize.Primary,
        defaultBackgroundColor = Dark600,
        defaultTextColor = Blue400,
        borders = true,
        onClick = null
    )
}
