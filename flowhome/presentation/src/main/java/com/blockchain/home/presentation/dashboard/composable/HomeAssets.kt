package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.MaskableText
import com.blockchain.componentlib.icons.Alert
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.QuestionOff
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.lazylist.paddedRoundedCornersItems
import com.blockchain.componentlib.tablerow.MaskableBalanceChangeTableRow
import com.blockchain.componentlib.tablerow.TableRowHeader
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.data.map
import com.blockchain.domain.paymentmethods.model.FundsLocks
import com.blockchain.home.presentation.allassets.CustodialAssetState
import com.blockchain.home.presentation.allassets.FiatAssetState
import com.blockchain.home.presentation.allassets.HomeAsset
import com.blockchain.home.presentation.allassets.HomeCryptoAsset
import com.blockchain.home.presentation.allassets.NonCustodialAssetState
import com.blockchain.home.presentation.allassets.composable.BalanceWithFiatAndCryptoBalance
import com.blockchain.home.presentation.allassets.composable.BalanceWithPriceChange
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money

@Composable
private fun FundLocksData(
    total: Money,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clickable(onClick = onClick),
        backgroundColor = AppTheme.colors.backgroundSecondary,
        shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(AppTheme.dimensions.smallSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(com.blockchain.stringResources.R.string.funds_locked_warning_title),
                style = AppTheme.typography.paragraph2,
                color = AppTheme.colors.muted
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallestSpacing))

            Image(Icons.QuestionOff.withTint(AppColors.muted).withSize(14.dp))

            Spacer(modifier = Modifier.weight(1F))

            MaskableText(
                text = total.toStringWithSymbol(),
                style = AppTheme.typography.paragraph2,
                color = AppTheme.colors.muted
            )
        }
    }
}

internal fun LazyListScope.homeAssets(
    locks: FundsLocks?,
    data: List<HomeAsset>,
    assetOnClick: (AssetInfo) -> Unit,
    openCryptoAssets: () -> Unit,
    fundsLocksOnClick: (FundsLocks) -> Unit,
    openFiatActionDetail: (String) -> Unit,
    showWarning: Boolean = false,
    warningOnClick: () -> Unit = {}
) {
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
        val showSeeAll = data.filterIsInstance<HomeCryptoAsset>().isNotEmpty()
        TableRowHeader(
            title = stringResource(com.blockchain.stringResources.R.string.ma_home_assets_title),
            icon = Icons.Filled.Alert.withTint(AppColors.dark).takeIf { showWarning },
            iconOnClick = warningOnClick,
            actionTitle = stringResource(com.blockchain.stringResources.R.string.see_all).takeIf { showSeeAll },
            actionOnClick = openCryptoAssets.takeIf { showSeeAll }
        )
    }

    locks?.let {
        paddedItem(
            paddingValues = {
                PaddingValues(
                    start = AppTheme.dimensions.smallSpacing,
                    end = AppTheme.dimensions.smallSpacing,
                    bottom = AppTheme.dimensions.tinySpacing
                )
            }
        ) {
            FundLocksData(
                total = locks.onHoldTotalAmount,
                onClick = { fundsLocksOnClick(it) }
            )
        }
    }

    paddedRoundedCornersItems(
        items = data.filterIsInstance<CustodialAssetState>(),
        key = { state -> state.asset.networkTicker },
        paddingValues = {
            PaddingValues(
                start = AppTheme.dimensions.smallSpacing,
                end = AppTheme.dimensions.smallSpacing,
                bottom = AppTheme.dimensions.smallSpacing
            )
        }
    ) {
        BalanceWithPriceChange(
            cryptoAsset = it,
            onAssetClick = assetOnClick
        )
    }

    paddedRoundedCornersItems(
        items = data.filterIsInstance<NonCustodialAssetState>(),
        key = { state -> state.asset.networkTicker },
        paddingValues = {
            PaddingValues(
                start = AppTheme.dimensions.smallSpacing,
                end = AppTheme.dimensions.smallSpacing,
                bottom = AppTheme.dimensions.smallSpacing
            )
        }
    ) {
        BalanceWithFiatAndCryptoBalance(
            cryptoAsset = it,
            onAssetClick = assetOnClick
        )
    }

    paddedRoundedCornersItems(
        items = data.filterIsInstance<FiatAssetState>(),
        key = { state -> state.account.currency.networkTicker },
        paddingValues = {
            PaddingValues(
                start = AppTheme.dimensions.smallSpacing,
                end = AppTheme.dimensions.smallSpacing,
                bottom = AppTheme.dimensions.smallSpacing
            )
        }
    ) {
        MaskableBalanceChangeTableRow(
            name = it.name,
            value = it.balance.map { money ->
                money.toStringWithSymbol()
            },
            imageResource = ImageResource.Remote(it.icon[0]),
            onClick = {
                openFiatActionDetail(it.account.currency.networkTicker)
            }
        )
    }
}
