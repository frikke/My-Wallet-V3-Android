package com.blockchain.componentlib.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.icon.CustomStackedIcon
import com.blockchain.componentlib.icons.ArrowRight
import com.blockchain.componentlib.icons.Coins
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Receive
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.system.ShimmerLoadingCard
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.stringResources.R

@Composable
private fun TwoAssetActionBody(
    start: @Composable RowScope.() -> Unit,
    end: @Composable RowScope.() -> Unit
) {
    Box {
        Row(modifier = Modifier.fillMaxWidth()) {
            start()
            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
            end()
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
                    iconSize = AppTheme.dimensions.standardSpacing
                )
            )
        }
    }
}

data class HorizontalAssetAction(
    val assetName: String,
    val icon: StackedIcon
)

@Composable
fun TwoAssetActionHorizontal(
    startTitle: String,
    start: HorizontalAssetAction,
    startOnClick: (() -> Unit)?,
    endTitle: String,
    end: HorizontalAssetAction?,
    endOnClick: (() -> Unit)?
) {
    TwoAssetActionBody(
        start = {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Ltr
            ) {
                start.run {
                    Asset(
                        modifier = Modifier.weight(1F),
                        title = startTitle,
                        subtitle = assetName,
                        icon = icon,
                        onClick = startOnClick
                    )
                }
            }
        },
        end = {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                end?.run {
                    Asset(
                        modifier = Modifier.weight(1F),
                        title = endTitle,
                        subtitle = assetName,
                        icon = icon,
                        onClick = endOnClick
                    )
                } ?: NoAsset(
                    modifier = Modifier.weight(1F),
                    title = endTitle,
                    onClick = endOnClick
                )
            }
        }
    )
}

@Composable
private fun Asset(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: StackedIcon,
    onClick: (() -> Unit)?
) {
    Surface(
        modifier = modifier,
        shape = AppTheme.shapes.large,
        color = AppTheme.colors.background
    ) {
        Row(
            modifier = Modifier
                .run {
                    if (onClick != null) {
                        clickable(onClick = onClick)
                    } else {
                        this
                    }
                }
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
fun NoAsset(
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)?
) {
    Asset(
        modifier = modifier,
        title = title,
        subtitle = stringResource(R.string.common_select),
        icon = StackedIcon.SingleIcon(Icons.Filled.Coins),
        onClick = onClick
    )
}

@Composable
fun TwoAssetActionHorizontalLoading() {
    TwoAssetActionBody(
        start = {
            Box(modifier = Modifier.weight(1F)) {
                ShimmerLoadingCard(
                    itemCount = 1,
                    showEndBlocks = false
                )
            }
        },
        end = {
            Box(modifier = Modifier.weight(1F)) {
                ShimmerLoadingCard(
                    itemCount = 1,
                    showEndBlocks = false,
                    reversed = true
                )
            }
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0XFFF1F2F7)
@Composable
private fun PreviewTwoAssetAction() {
    TwoAssetActionHorizontal(
        startTitle = "From",
        start = HorizontalAssetAction(
            assetName = "ETH",
            StackedIcon.SingleIcon(Icons.Receive)
        ),
        startOnClick = {},
        endTitle = "To",
        end = HorizontalAssetAction(
            assetName = "BTC",
            StackedIcon.SingleIcon(Icons.Receive)
        ),
        endOnClick = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0XFFF1F2F7)
@Composable
private fun PreviewTwoAssetAction_Select() {
    TwoAssetActionHorizontal(
        startTitle = "From",
        start = HorizontalAssetAction(
            assetName = "ETH",
            StackedIcon.SingleIcon(Icons.Receive)
        ),
        startOnClick = {},
        endTitle = "To",
        end = null,
        endOnClick = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0XFFF1F2F7)
@Composable
private fun PreviewTwoAssetActionLoading() {
    TwoAssetActionHorizontalLoading()
}
