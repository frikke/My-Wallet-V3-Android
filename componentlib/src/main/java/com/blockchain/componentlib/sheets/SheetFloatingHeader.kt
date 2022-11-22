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
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icon.CustomStackedIcon
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.clickableNoEffect

@Composable
fun SheetFloatingHeader(
    icon: StackedIcon,
    title: String,
    onCloseClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(AppTheme.dimensions.tinySpacing)
            .fillMaxWidth(),
        shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium),
        elevation = AppTheme.dimensions.borderRadiiSmallest,
        backgroundColor = AppTheme.colors.light
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppTheme.dimensions.tinySpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))

            Row(
                modifier = Modifier.weight(1F),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomStackedIcon(
                    icon = icon,
                    borderColor = AppTheme.colors.light
                )

                Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

                Text(
                    modifier = Modifier.padding(vertical = AppTheme.dimensions.verySmallSpacing),
                    text = title,
                    style = AppTheme.typography.body2,
                    color = AppTheme.colors.title,
                )
            }

            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

            Image(
                modifier = Modifier.clickableNoEffect { onCloseClick() },
                imageResource = ImageResource.Local(R.drawable.ic_close_circle_white)
            )
        }
    }
}

@Preview(backgroundColor = 0xF0F2F7CC, showBackground = true)
@Composable
fun PreviewSheetFloatingHeader_Overlap() {
    SheetFloatingHeader(
        icon = StackedIcon.OverlappingPair(
            front = ImageResource.Local(R.drawable.ic_close_circle_dark),
            back = ImageResource.Local(R.drawable.ic_close_circle),
        ),
        title = "Swapped BTC -> ETH",
        onCloseClick = {}
    )
}

@Preview(backgroundColor = 0xF0F2F7CC, showBackground = true)
@Composable
fun PreviewSheetFloatingHeader_SmallTag() {
    SheetFloatingHeader(
        icon = StackedIcon.SmallTag(
            main = ImageResource.Local(R.drawable.ic_close_circle_dark),
            tag = ImageResource.Local(R.drawable.ic_close_circle),
        ),
        title = "Swapped BTC -> ETH",
        onCloseClick = {}
    )
}

@Preview(backgroundColor = 0xF0F2F7CC, showBackground = true)
@Composable
fun PreviewSheetFloatingHeader_Single() {
    SheetFloatingHeader(
        icon = StackedIcon.SingleIcon(
            ImageResource.Local(R.drawable.ic_close_circle_dark)
        ),
        title = "Swapped BTC -> ETH",
        onCloseClick = {}
    )
}
