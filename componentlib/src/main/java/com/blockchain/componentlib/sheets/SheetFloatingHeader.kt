package com.blockchain.componentlib.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icon.CustomStackedIcon
import com.blockchain.componentlib.icons.ChevronLeft
import com.blockchain.componentlib.icons.Close
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.clickableNoEffect
import com.blockchain.componentlib.utils.conditional

@Composable
private fun SheetHeader(
    color: Color = AppTheme.colors.background,
    icon: StackedIcon,
    title: String,
    isFloating: Boolean,
    backOnClick: (() -> Unit)? = null,
    onCloseClick: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier
            .padding(if (isFloating) AppTheme.dimensions.tinySpacing else 0.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium),
        elevation = if (isFloating) AppTheme.dimensions.borderRadiiSmallest else 0.dp,
        backgroundColor = color
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (isFloating) {
                    AppTheme.dimensions.tinySpacing
                } else {
                    AppTheme.dimensions.smallSpacing
                }
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            backOnClick?.let {
                Image(
                    modifier = Modifier.clickableNoEffect { backOnClick() },
                    imageResource = Icons.ChevronLeft
                )
                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
            } ?: Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))

            Row(
                modifier = Modifier.weight(1F),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomStackedIcon(
                    icon = icon,
                    borderColor = color
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

                Text(
                    modifier = Modifier.padding(
                        vertical = AppTheme.dimensions.verySmallSpacing
                    ),
                    text = title,
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.title
                )
            }

            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

            Image(
                modifier = Modifier.conditional(onCloseClick != null) {
                    clickableNoEffect { onCloseClick?.invoke() }
                },
                imageResource = onCloseClick?.let {
                    Icons.Close
                        .withTint(AppColors.body)
                        .withBackground(
                            backgroundColor = AppColors.backgroundSecondary,
                            iconSize = AppTheme.dimensions.standardSpacing,
                            backgroundSize = AppTheme.dimensions.standardSpacing
                        )
                } ?: ImageResource.None
            )
        }
    }
}

@Composable
fun SheetFloatingHeader(
    color: Color = AppTheme.colors.background,
    icon: StackedIcon,
    title: String,
    backOnClick: (() -> Unit)? = null,
    onCloseClick: (() -> Unit)? = null,
) {
    SheetHeader(
        color = color,
        icon = icon,
        title = title,
        isFloating = true,
        backOnClick = backOnClick,
        onCloseClick = onCloseClick
    )
}

@Composable
fun SheetFlatHeader(
    color: Color = AppTheme.colors.background,
    icon: StackedIcon,
    title: String,
    backOnClick: (() -> Unit)? = null,
    onCloseClick: (() -> Unit)? = null,
) {
    SheetHeader(
        color = color,
        icon = icon,
        title = title,
        isFloating = false,
        backOnClick = backOnClick,
        onCloseClick = onCloseClick
    )
}

@Preview(backgroundColor = 0xF0F2F7CC, showBackground = true)
@Composable
private fun PreviewSheetFloatingHeader_Overlap() {
    SheetFloatingHeader(
        icon = StackedIcon.OverlappingPair(
            front = ImageResource.Local(R.drawable.ic_close_circle_dark),
            back = ImageResource.Local(R.drawable.ic_close_circle)
        ),
        title = "Swapped BTC -> ETH",
        backOnClick = {}
    )
}

@Preview(backgroundColor = 0xF0F2F7CC, showBackground = true)
@Composable
private fun PreviewSheetFloatingHeader_SmallTag() {
    SheetFloatingHeader(
        icon = StackedIcon.SmallTag(
            main = ImageResource.Local(R.drawable.ic_close_circle_dark),
            tag = ImageResource.Local(R.drawable.ic_close_circle)
        ),
        title = "Swapped BTC -> ETH"
    )
}

@Preview(backgroundColor = 0xF0F2F7CC, showBackground = true)
@Composable
private fun PreviewSheetFloatingHeader_Single() {
    SheetFloatingHeader(
        icon = StackedIcon.SingleIcon(
            ImageResource.Local(R.drawable.ic_close_circle_dark)
        ),
        title = "Swapped BTC -> ETH",
        onCloseClick = {}
    )
}

@Preview(backgroundColor = 0xF0F2F7CC, showBackground = true)
@Composable
private fun PreviewSheetFlatHeader_Single() {
    SheetFlatHeader(
        icon = StackedIcon.SingleIcon(
            ImageResource.Local(R.drawable.ic_close_circle_dark)
        ),
        title = "Swapped BTC -> ETH",
        backOnClick = {},
        onCloseClick = {}
    )
}
