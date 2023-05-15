package com.blockchain.home.presentation.swapdexoption

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.lazylist.roundedCornersItems
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.navigation.NavigationBarButton
import com.blockchain.componentlib.tablerow.TableRow
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.BackgroundMuted
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.Grey700
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.home.presentation.R
import com.blockchain.koin.newSwapFlowFeatureFlag
import org.koin.androidx.compose.get

@Composable
fun SwapDexOptionScreen(
    newSwapFlowFF: FeatureFlag = get(newSwapFlowFeatureFlag),
    onBackPressed: () -> Unit,
    openSwap: (newFlow: Boolean) -> Unit,
    openDex: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = BackgroundMuted),
    ) {
        NavigationBar(
            title = stringResource(id = com.blockchain.stringResources.R.string.select_an_option),
            navigationBarButtons = listOf(
                NavigationBarButton.IconResource(
                    ImageResource.Local(com.blockchain.componentlib.R.drawable.ic_close_circle),
                    onIconClick = { onBackPressed() }
                )
            )
        )
        Text(
            modifier = Modifier.padding(
                start = AppTheme.dimensions.smallSpacing,
                top = AppTheme.dimensions.standardSpacing,
                bottom = AppTheme.dimensions.tinySpacing
            ),
            text = stringResource(id = com.blockchain.stringResources.R.string.choose_how_to_swap),
            style = AppTheme.typography.body2,
            color = Grey700
        )
        val items = listOf(
            SwapOption(
                title = stringResource(id = com.blockchain.stringResources.R.string.bcdc_swap),
                subtitle = stringResource(id = com.blockchain.stringResources.R.string.cross_chain_limited_tokens),
                type = SwapType.BCDC_SWAP,
            ),
            SwapOption(
                title = stringResource(id = com.blockchain.stringResources.R.string.dex_swap),
                subtitle = stringResource(id = com.blockchain.stringResources.R.string.single_chain_eth_tokens),
                type = SwapType.DEX,

            )
        )

        // TODO(aromano): TEMP FF | null = do not navigate, true = navigate to new flow, false = navigate to old flow
        var navigateToSwapFlow by remember { mutableStateOf(false) }
        LaunchedEffect(navigateToSwapFlow) {
            if (navigateToSwapFlow) {
                openSwap(newSwapFlowFF.coEnabled())
                navigateToSwapFlow = false
            }
        }

        LazyColumn(Modifier.padding(horizontal = AppTheme.dimensions.smallSpacing)) {
            roundedCornersItems(
                items = items,
            ) {
                SwapOptionCell(
                    item = it,
                    openDex = openDex,
                    openSwap = { navigateToSwapFlow = true },
                    close = onBackPressed
                )
            }
        }
    }
}

@Composable
private fun SwapOptionCell(
    item: SwapOption,
    openSwap: () -> Unit,
    openDex: () -> Unit,
    close: () -> Unit,
) {
    TableRow(
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Row {
                    Text(
                        text = item.title,
                        style = AppTheme.typography.body2,
                        modifier = Modifier.weight(1f),
                        color = AppTheme.colors.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = item.subtitle,
                    style = AppTheme.typography.paragraph1,
                    color = AppTheme.colors.muted
                )
            }
        },
        contentEnd = {
            Image(
                imageResource = ImageResource.Local(
                    id = com.blockchain.componentlib.R.drawable.ic_chevron_end,
                    colorFilter = ColorFilter.tint(Grey400)
                ),
                modifier = Modifier.requiredSizeIn(
                    maxWidth = dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing),
                    maxHeight = dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing),
                )
            )
        },
        contentStart = {
            Image(
                imageResource = when (item.type) {
                    SwapType.BCDC_SWAP -> ImageResource.Local(R.drawable.ic_swap_option)
                    SwapType.DEX -> ImageResource.Local(R.drawable.ic_dex_option)
                },
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = dimensionResource(com.blockchain.common.R.dimen.medium_spacing))
                    .size(dimensionResource(com.blockchain.common.R.dimen.standard_spacing)),
            )
        },
        contentBottom = {
            Image(
                modifier = Modifier.padding(
                    start = 44.dp,
                    top = AppTheme.dimensions.verySmallSpacing
                ),
                imageResource = ImageResource.Local(
                    when (item.type) {
                        SwapType.BCDC_SWAP -> R.drawable.ic_bcdc_swap_currencies
                        SwapType.DEX -> R.drawable.ic_dex_currencies
                    }
                )
            )
        },
        onContentClicked = {
            close()
            when (item.type) {
                SwapType.BCDC_SWAP -> openSwap()
                SwapType.DEX -> openDex()
            }
        }
    )
}

private data class SwapOption(
    val title: String,
    val subtitle: String,
    val type: SwapType
)

private enum class SwapType {
    BCDC_SWAP, DEX
}

@Preview
@Composable
private fun SwapDexOptionItemPreview() {
    AppTheme {
        AppSurface {
            SwapOptionCell(
                item = SwapOption(
                    "Title",
                    "Cross-chain, limited token pairs",
                    SwapType.BCDC_SWAP
                ),
                {}, {}, {}
            )
        }
    }
}

@Preview
@Composable
private fun SwapDexOptionPreview() {
    AppTheme {
        AppSurface {
            SwapDexOptionScreen(
                onBackPressed = {},
                openSwap = {},
                openDex = {}
            )
        }
    }
}
