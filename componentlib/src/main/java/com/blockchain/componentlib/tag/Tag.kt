package com.blockchain.componentlib.tag

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
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue400
import com.blockchain.componentlib.theme.Dark600
import com.blockchain.componentlib.theme.Grey000

@Composable
fun Tag(
    modifier: Modifier = Modifier,
    text: String,
    size: TagSize,
    defaultBackgroundColor: Color,
    defaultTextColor: Color,
    borders: Boolean = false,
    startImageResource: ImageResource = ImageResource.None,
    endImageResource: ImageResource = ImageResource.Local(
        id = R.drawable.ic_chevron_end_small,
        colorFilter = ColorFilter.tint(defaultTextColor, blendMode = BlendMode.SrcAtop)
    ),
    onClick: (() -> Unit)?
) {

    val paddingHorizontal = when (size) {
        TagSize.Primary -> 8.dp
        TagSize.Large -> dimensionResource(R.dimen.very_small_spacing)
    }

    val paddingVertical = when (size) {
        TagSize.Primary -> dimensionResource(R.dimen.smallest_spacing)
        TagSize.Large -> dimensionResource(R.dimen.minuscule_spacing)
    }

    val textStyle = when (size) {
        TagSize.Primary -> AppTheme.typography.caption2
        TagSize.Large -> AppTheme.typography.paragraph2
    }

    Row(
        modifier = modifier
            .then(
                Modifier
                    .border(
                        width = 1.dp,
                        color = Grey000,
                        shape = RoundedCornerShape(size = dimensionResource(R.dimen.smallest_spacing)),
                    )
                    .takeIf { borders } ?: Modifier
            )
            .clip(RoundedCornerShape(size = dimensionResource(R.dimen.smallest_spacing)))
            .background(defaultBackgroundColor)
            .padding(horizontal = paddingHorizontal, vertical = paddingVertical)
            .then(
                onClick?.let { Modifier.clickable(onClick = onClick) } ?: Modifier
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (startImageResource != ImageResource.None) {
            Image(imageResource = startImageResource)
            Spacer(modifier = Modifier.width(width = AppTheme.dimensions.smallestSpacing))
        }

        Text(
            text = text,
            style = textStyle,
            color = defaultTextColor
        )

        if (endImageResource != ImageResource.None && onClick != null) {
            Spacer(modifier = Modifier.width(width = dimensionResource(R.dimen.tiny_spacing)))
            Image(imageResource = endImageResource)
        }
    }
}

@Preview
@Composable
fun ClickableTag() {
    AppTheme(darkTheme = false) {
        AppSurface {
            Tag(
                text = "Click me",
                size = TagSize.Primary,
                defaultBackgroundColor = Dark600,
                defaultTextColor = Blue400,
                borders = false,
                startImageResource = ImageResource.Local(
                    id = R.drawable.ic_info_outline,
                    colorFilter = ColorFilter.tint(Blue400, blendMode = BlendMode.SrcAtop)
                ),
                onClick = null,
            )
        }
    }
}

@Preview
@Composable
fun NonClickableTag() {
    AppTheme(darkTheme = false) {
        AppSurface {
            Tag(
                text = "Click me",
                size = TagSize.Primary,
                defaultBackgroundColor = Dark600,
                defaultTextColor = Blue400,
                borders = true,
                onClick = { },
            )
        }
    }
}
