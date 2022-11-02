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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icon.OverlapIcons
import com.blockchain.componentlib.icon.SmallTagIcons
import com.blockchain.componentlib.theme.AppTheme

sealed interface ImageType {
    data class OverlapIcons(
        val front: ImageResource,
        val back: ImageResource
    ) : ImageType

    data class SmallTagIcons(
        val main: ImageResource,
        val tag: ImageResource
    ) : ImageType

    data class SingleIcon(
        val icon: ImageResource
    ) : ImageType
}

@Composable
fun SheetFloatingHeader(
    imageType: ImageType,
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
                when (imageType) {
                    is ImageType.OverlapIcons -> {
                        OverlapIcons(front = imageType.front, back = imageType.back)
                    }
                    is ImageType.SmallTagIcons -> {
                        SmallTagIcons(main = imageType.main, tag = imageType.tag)
                    }
                    is ImageType.SingleIcon -> {
                        Image(imageResource = imageType.icon)
                    }
                }

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
                imageResource = ImageResource.Local(   R.drawable.ic_close_circle_white  )
            )
        }
    }
}

@Preview(backgroundColor = 0xF0F2F7CC, showBackground = true)
@Composable
fun PreviewSheetFloatingHeader_Overlap() {
    SheetFloatingHeader(
        imageType = ImageType.OverlapIcons(
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
        imageType = ImageType.SmallTagIcons(
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
        imageType = ImageType.SingleIcon(
            ImageResource.Local(R.drawable.ic_close_circle_dark)
        ),
        title = "Swapped BTC -> ETH",
        onCloseClick = {}
    )
}
