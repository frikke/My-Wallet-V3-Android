package com.blockchain.componentlib.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.icon.CustomStackedIcon
import com.blockchain.componentlib.icons.ArrowRight
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Receive
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun TwoAssetAction(
    startTitle: String,
    startSubtitle: String,
    startIcon: StackedIcon,
    endTitle: String,
    endSubtitle: String,
    endIcon: StackedIcon,
) {
    Box {
        Row(modifier = Modifier.fillMaxWidth()) {
            AssetStart(
                modifier = Modifier.weight(1F),
                title = startTitle,
                subtitle = startSubtitle,
                icon = startIcon,
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

            AssetEnd(
                modifier = Modifier.weight(1F),
                title = endTitle,
                subtitle = endSubtitle,
                icon = endIcon,
            )
        }

        Surface(
            modifier = Modifier
                .size(AppTheme.dimensions.hugeSpacing)
                .align(Alignment.Center),
            shape = CircleShape,
            border = BorderStroke(AppTheme.dimensions.tinySpacing, AppTheme.colors.backgroundMuted)
        ) {
            Image(
                imageResource = Icons.ArrowRight.withBackground(
                    backgroundColor = Color.White,
                    backgroundSize = AppTheme.dimensions.standardSpacing,
                    iconSize = AppTheme.dimensions.standardSpacing,
                )
            )
        }
    }
}

@Composable
private fun AssetStart(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: StackedIcon
) {
    Surface(
        modifier = modifier,
        shape = AppTheme.shapes.large,
        color = AppTheme.colors.background
    ) {
        Row(
            modifier = Modifier
                .padding(AppTheme.dimensions.smallSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomStackedIcon(icon = icon)

            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

            Column {
                Text(
                    text = title,
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.title
                )
                Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))
                Text(
                    text = subtitle,
                    style = AppTheme.typography.paragraph1,
                    color = AppTheme.colors.body
                )
            }
        }
    }
}

@Composable
private fun AssetEnd(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: StackedIcon
) {
    Surface(
        modifier = modifier,
        shape = AppTheme.shapes.large,
        color = AppTheme.colors.background
    ) {
        Row(
            modifier = Modifier
                .padding(AppTheme.dimensions.smallSpacing),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Column {
                Text(
                    text = title,
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.title
                )
                Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))
                Text(
                    text = subtitle,
                    style = AppTheme.typography.paragraph1,
                    color = AppTheme.colors.body
                )
            }

            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

            CustomStackedIcon(icon = icon)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0XFFF1F2F7)
@Composable
private fun PreviewTwoAssetAction() {
    TwoAssetAction(
        startTitle = "From",
        startSubtitle = "ETH",
        startIcon = StackedIcon.SingleIcon(Icons.Receive),
        endTitle = "To",
        endSubtitle = "BTC",
        endIcon = StackedIcon.SingleIcon(Icons.Receive)
    )
}

@Preview
@Composable
private fun PreviewAssetStart() {
    AssetStart(
        title = "From",
        subtitle = "ETH",
        icon = StackedIcon.SingleIcon(Icons.Receive)
    )
}
