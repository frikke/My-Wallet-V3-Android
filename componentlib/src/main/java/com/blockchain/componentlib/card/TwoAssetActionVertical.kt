package com.blockchain.componentlib.card

import androidx.compose.foundation.BorderStroke
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
import com.blockchain.componentlib.icons.ArrowDown
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Receive
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallHorizontalSpacer
import com.blockchain.componentlib.theme.SmallestVerticalSpacer

@Composable
fun TwoAssetAction(
    topTitle: String,
    topSubtitle: String,
    topEndTitle: String,
    topEndSubtitle: String,
    topIcon: StackedIcon,
    bottomTitle: String,
    bottomSubtitle: String,
    bottomEndTitle: String,
    bottomEndSubtitle: String,
    bottomIcon: StackedIcon,
) {
    Box {
        Column(modifier = Modifier.fillMaxWidth()) {
            Asset(
                title = topTitle,
                subtitle = topSubtitle,
                endTitle = topEndTitle,
                endSubtitle = topEndSubtitle,
                icon = topIcon
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

            Asset(
                title = bottomTitle,
                subtitle = bottomSubtitle,
                endTitle = bottomEndTitle,
                endSubtitle = bottomEndSubtitle,
                icon = bottomIcon
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
                imageResource = Icons.ArrowDown.withBackground(
                    backgroundColor = Color.White,
                    backgroundSize = AppTheme.dimensions.standardSpacing,
                    iconSize = AppTheme.dimensions.standardSpacing,
                )
            )
        }
    }
}

@Composable
private fun Asset(
    title: String,
    subtitle: String,
    endTitle: String,
    endSubtitle: String,
    icon: StackedIcon
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.large,
        color = AppTheme.colors.background
    ) {
        Row(
            modifier = Modifier
                .padding(AppTheme.dimensions.smallSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomStackedIcon(icon = icon)

            SmallHorizontalSpacer()

            Column(
                modifier = Modifier.weight(weight = 1F, fill = true),
            ) {
                Text(
                    text = title,
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.title
                )
                SmallestVerticalSpacer()
                Text(
                    text = subtitle,
                    style = AppTheme.typography.paragraph1,
                    color = AppTheme.colors.body
                )
            }

            SmallHorizontalSpacer()

            Column(
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = endTitle,
                    style = AppTheme.typography.paragraph2,
                    color = AppTheme.colors.title
                )
                SmallestVerticalSpacer()
                Text(
                    text = endSubtitle,
                    style = AppTheme.typography.paragraph1,
                    color = AppTheme.colors.body
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0XFFF1F2F7)
@Composable
private fun PreviewTwoAssetAction() {
    TwoAssetAction(
        topTitle = "From",
        topSubtitle = "Ethereum",
        topEndTitle = "0.05459411 ETH",
        topEndSubtitle = "100.00",
        topIcon = StackedIcon.SingleIcon(Icons.Receive),
        bottomTitle = "To",
        bottomSubtitle = "Bitcoin",
        bottomEndTitle = "0.00350795 BTC",
        bottomEndSubtitle = "96.99",
        bottomIcon = StackedIcon.SingleIcon(Icons.Receive),
    )
}

@Preview
@Composable
private fun PreviewAssetStart() {
    Asset(
        title = "From",
        subtitle = "ETH",
        endTitle = "0.05459411 ETH",
        endSubtitle = "100.00",
        icon = StackedIcon.SingleIcon(Icons.Receive)
    )
}
