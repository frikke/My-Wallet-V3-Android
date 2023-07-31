package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.res.stringResource
import com.blockchain.componentlib.icons.Fire
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.tablerow.TableRowHeader
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.data.DataResource
import com.blockchain.prices.prices.PriceItemViewState
import com.blockchain.prices.prices.composable.TopMoversScreen
import info.blockchain.balance.AssetInfo
import kotlinx.collections.immutable.ImmutableList

internal fun LazyListScope.homeTopMovers(
    data: DataResource<ImmutableList<PriceItemViewState>>,
    assetOnClick: (AssetInfo) -> Unit
) {
    (data as? DataResource.Data)?.data
        ?.takeIf { it.isNotEmpty() }
        ?.let {
            paddedItem(
                paddingValues = {
                    PaddingValues(
                        start = AppTheme.dimensions.smallSpacing,
                        end = AppTheme.dimensions.smallSpacing,
                        top = AppTheme.dimensions.smallSpacing,
                        bottom = AppTheme.dimensions.tinySpacing
                    )
                }
            ) {
                TableRowHeader(
                    title = stringResource(com.blockchain.stringResources.R.string.prices_top_movers),
                    icon = Icons.Filled.Fire
                        .withSize(AppTheme.dimensions.smallSpacing)
                        .withTint(AppTheme.colors.warningMuted)
                )
            }

            paddedItem(
                paddingValues = {
                    PaddingValues(
                        bottom = AppTheme.dimensions.smallSpacing
                    )
                }
            ) {
                TopMoversScreen(
                    data = data,
                    assetOnClick = assetOnClick
                )
            }
        }
}
